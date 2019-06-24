package com.jianglei.asmplugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.tools.r8.com.google.gson.Gson
import com.google.common.collect.ImmutableSet
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class MethodTraceTransform extends Transform {

    private Project project
    private boolean isForApplication

    MethodTraceTransform(Project project, boolean isForApplication) {
        this.project = project
        this.isForApplication = isForApplication
    }

    @Override
    String getName() {
        return "MethodTrace"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {

        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        def scopes = new HashSet()
        scopes.add(QualifiedContent.Scope.PROJECT)
        if (isForApplication) {
            //application module中加入此项可以处理第三方jar包
            scopes.add(QualifiedContent.Scope.EXTERNAL_LIBRARIES)
        }
        return scopes
    }

    @Override
    boolean isIncremental() {
        return true
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        //获取配置信息
        MethodTraceExtension extension = project.getExtensions().findByType(MethodTraceExtension.class)
        LogUtils.e(" configure:" + extension.traceThirdLibrary)

        def gson = new Gson()
        //保存上次依赖jar文件和输出的jar文件的依赖关系，比如上次编译时gson被输出成32.jar
        def lastConfigFile = new File(transformInvocation.context.temporaryDir.absolutePath + File.separator + "config.json")
        //用来保存jar文件的名称和输出路径的映射
        def lastJarMap = new HashMap<String, String>()
        //此次是否是增量编译
        def isIncrement = false
        //如果是增量编译，gradle配置是否改变,如果改变要全部重来
        def isConfigChange = false
        LastConfig lastConfig = null
        if (lastConfigFile.exists()) {
            isIncrement = true
            def lines = FileUtils.readLines(lastConfigFile)
            if (lines == null || lines.size() == 0 || lines.size() > 1) {
                throw IllegalStateException("bad config file ,please clean the project and rebuild it")
            }
            def json = lines.get(0)
            try {
                lastConfig = gson.fromJson(json, LastConfig.class)
                if (lastConfig == null || lastConfig.extension == null) {
                    //gson文件损坏
                    isIncrement = false
                } else {
                    lastJarMap = lastConfig.jarMap
                    isConfigChange = lastConfig.extension.traceThirdLibrary != extension.traceThirdLibrary
                    LogUtils.e("isConfigChange:" + isConfigChange)
                }
            } catch (Exception e) {
                e.printStackTrace()
                isIncrement = false
            }
        } else {
            isIncrement = false
        }
        def outputDir = getOutputDir(transformInvocation)
        if (isIncrement && isConfigChange) {
            DirectoryUtils.deleteAllJars(outputDir)
        }
        //此次编译参与的所有jar，记录下来和上次参与的jar对比，删掉多余的文件
        def curJars = new HashSet<String>()
        transformInvocation.inputs
                .each { input ->
            transformSrc(transformInvocation, input, isIncrement, isConfigChange)
            transformJar(transformInvocation, input, isIncrement,
                    isConfigChange, lastJarMap, curJars, extension)
        }
        if (isIncrement) {

            //依赖有改变后，比如删除了某个依赖，假如该依赖之前生成的是32.jar,由于
            //删除某个依赖后我们这里是收不到通知的，所以这个32.jar在增量构建时依然存在，
            //下次我们将这个依赖重新加回来，可能会生成34.jar,这个时候32.jar和34.jar其实
            //内容一模一样，编译时会报类冲突，所以这里我们坐下检查，删除多余的jar文件
            def iterator = lastJarMap.entrySet().iterator()
            while (iterator.hasNext()) {
                def entry = iterator.next()
                if (!curJars.contains(entry.key)) {
                    iterator.remove()
                    new File(entry.value).delete()
                    LogUtils.i("delete jar:" + entry.key + " " + entry.value)
                }
            }
        }
        //将映射关系写入到文件，下次使用
        if (lastConfig == null) {
            lastConfig = new LastConfig()
        }
        //此处获得的extension是经过gradle修改的，不是纯粹的MethodTraceExtension类，
        //json处理会包异常，所以我们新建一个新的类
        lastConfig.extension = new MethodTraceExtension(extension)
        lastConfig.jarMap = lastJarMap
        def lines = Arrays.asList(gson.toJson(lastConfig))
        FileUtils.writeLines(lastConfigFile, lines)
    }

    private void transformSrc(TransformInvocation transformInvocation, TransformInput input,
                              boolean isIncrement, boolean isConfigChange) {

        input.directoryInputs.each { directoryInput ->

            def outputDirFile = transformInvocation.outputProvider.getContentLocation(
                    directoryInput.name, directoryInput.contentTypes, directoryInput.scopes,
                    Format.DIRECTORY
            )
            def outputFilePath = outputDirFile.absolutePath
            def inputFilePath = directoryInput.file.absolutePath
            if (isIncrement && !isConfigChange && directoryInput.changedFiles.size() != 0) {
                //增量编译且配置文件没有改变
                LogUtils.i("增量编译，且配置文件没有改变")
                directoryInput.changedFiles.each { changeFile ->
                    def outputFullPath = changeFile.key.absolutePath.replace(inputFilePath, outputFilePath)
                    def outputFile = new File(outputFullPath)
                    if (!outputFile.parentFile.exists()) {
                        outputFile.parentFile.mkdirs()
                    }
                    if ((changeFile.value == Status.CHANGED || changeFile.value == Status.ADDED)
                            && !changeFile.key.isDirectory()) {
                        //有时候新增一个module，该module目录会传进来，对于该目录我们无需处理
                        LogUtils.i("dir change files:" + changeFile.key.absolutePath + "  " + changeFile.value.toString())
                        MethodTraceUtils.traceFile(changeFile.key, outputFile)
                    } else if (changeFile.value == Status.REMOVED) {
                        outputFile.delete()
                    }
                }
            } else {
                //改动文件为空有两种情况，一种是第一次构建或clean后重新构建，另一种就是移除或新增某个依赖
                //为了区分这种情况，我们判断输出目录下是否已有目录，如果有的话，说明不是第一种情况，而是第二种，这个
                //时候我们不需要再处理这种情况了
                if (!DirectoryUtils.hasDir(outputFilePath)) {
                    def allFiles = DirectoryUtils.getAllFiles(directoryInput.file)
                    for (File file : allFiles) {
                        def outputFullPath = file.absolutePath.replace(inputFilePath, outputFilePath)
                        def outputFile = new File(outputFullPath)
                        if (!outputFile.parentFile.exists()) {
                            outputFile.parentFile.mkdirs()
                        }
                        MethodTraceUtils.traceFile(file, outputFile)
                    }
                }
            }
        }
    }

    private void transformJar(TransformInvocation transformInvocation, TransformInput input,
                              boolean isIncrement, boolean isConfigChange,
                              Map<String, String> lastJarMap, Set<String> curJars, MethodTraceExtension extension) {

        for (JarInput jarInput : input.jarInputs) {
            def outputFile = transformInvocation.outputProvider.getContentLocation(
                    jarInput.name, jarInput.contentTypes, jarInput.scopes,
                    Format.JAR
            )
            if (!isIncrement) {
                //第一次编译，记录名称和路径的映射关系
                lastJarMap.put(jarInput.name, outputFile.absolutePath)
            }
            if (!isIncrement || isConfigChange) {
                //非增量情况，或者gradle配置文件有改动，都全部重新来过
                if (extension.traceThirdLibrary) {
                    MethodTraceUtils.traceJar(jarInput, outputFile)
                } else {
                    FileUtils.copyFile(jarInput.file, outputFile)
                }
                curJars.add(jarInput.name)
            } else {
                //增量情况
                def jaroutDir = outputFile.absolutePath
                if (jarInput.status == Status.ADDED || jarInput.status == Status.CHANGED) {
                    if (extension.traceThirdLibrary) {
                        MethodTraceUtils.traceJar(jarInput, outputFile)
                    } else {
                        FileUtils.copyFile(jarInput.file, outputFile)
                    }
                    lastJarMap.put(jarInput.name, jaroutDir)
                    curJars.add(jarInput.name)
                } else if (jarInput.status == Status.NOTCHANGED) {
                    //记录当前哪些jar参与了编译
                    curJars.add(jarInput.name)
                } else {
                    //Status.REMOVED,其实一般删除一个jar，实测并不会传入进来,所以什么都不干
                    LogUtils.i("jar deleted:" + jarInput.name + "  ")
                }
            }
        }
    }


    /**
     * 获取输出目录
     */
    private String getOutputDir(TransformInvocation invocation) {
        def outputFile = invocation.outputProvider.getContentLocation(
                "test", ImmutableSet.of(QualifiedContent.DefaultContentType.CLASSES),
                TransformManager.SCOPE_FULL_PROJECT,
                Format.JAR
        )
        return outputFile.parentFile.absolutePath
    }

}