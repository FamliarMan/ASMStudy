package com.jianglei.asmplugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.tools.r8.com.google.gson.Gson
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

class MethodTraceTransform extends Transform {

    private Project project

    MethodTraceTransform(Project project) {
        this.project = project
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
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return true
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {


        def gson = new Gson()
        //保存上次依赖jar文件和输出的jar文件的依赖关系，比如上次编译时gson被输出成32.jar
        def lastConfigFile = new File(transformInvocation.context.temporaryDir.absolutePath + "config.json")
        //用来保存jar文件的名称和输出路径的映射
        def jarMap = new HashMap<String, String>()
        //此次是否是增量编译
        def isIncrement = false
        if (lastConfigFile.exists()) {
            isIncrement = true
            def lines = FileUtils.readLines(lastConfigFile)
            if (lines == null || lines.size() == 0 || lines.size() > 1) {
                throw IllegalStateException("bad config file ,please clean the project and rebuild it")
            }
            def json = lines.get(0)
            jarMap = gson.fromJson(json, HashMap.class)
        } else {
            isIncrement = false
        }
        //所有有用的jar文件路径保存，用来删除多余的jar
        def jarFiles = new HashSet<String>()
        def jaroutDir = ""
        transformInvocation.inputs
                .each { input ->

            input.directoryInputs.each { directoryInput ->

                def outputDirFile = transformInvocation.outputProvider.getContentLocation(
                        directoryInput.name, directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY
                )
                def outputFilePath = outputDirFile.absolutePath
                def inputFilePath = directoryInput.file.absolutePath
                if (directoryInput.changedFiles.size() != 0) {
                    //增量编译
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
                            LogUtils.i("dir first:" + outputFullPath)
                            MethodTraceUtils.traceFile(file, outputFile)
                        }
                    }
                }
            }


            for (JarInput jarInput : input.jarInputs) {
                def outputFile = transformInvocation.outputProvider.getContentLocation(
                        jarInput.name, jarInput.contentTypes, jarInput.scopes,
                        Format.JAR
                )
                if (!isIncrement) {
                    //第一次编译，记录名称和路径的映射关系
                    jarMap.put(jarInput.name, outputFile.absolutePath)
                }
                jaroutDir = outputFile.absolutePath
                jarFiles.add(outputFile.absolutePath)
                LogUtils.i("get jar:" + outputFile.absolutePath + "  " + jarInput.name)
                if (jarInput.status == Status.ADDED || jarInput.status == Status.CHANGED) {
                    MethodTraceUtils.traceJar(jarInput.file, outputFile)
                    if (isIncrement) {
                        //增量编译，更新对应关系
                        jarMap.put(jarInput.name, jaroutDir)
                    }
                    LogUtils.i("jar changed: " + outputFile.absolutePath)

                } else if (jarInput.status == Status.NOTCHANGED) {
                    if (!isIncrement) {
                        //第一次或者clean后的编译,这里要插入字节码操作代码,这里暂时直接复制过去
                        MethodTraceUtils.traceJar(jarInput.file, outputFile)
                    } else {
                        //什么都不用做
                    }
                } else {
                    //Status.REMOVED,其实一般删除一个jar，实测并不会传入进来,所以这里什么都不做
                }
            }

            LogUtils.i("get jar:---------------------------------------")
        }
        //将映射关系写入到文件，下次使用
        def lines = Arrays.asList(gson.toJson(jarMap))
        FileUtils.writeLines(lastConfigFile, lines)
        //依赖有改变后，比如删除了某个依赖，假如该依赖之前生成的是32.jar,由于
        //删除某个依赖后我们这里是收不到通知的，所以这个32.jar在增量构建时依然存在，
        //下次我们将这个依赖重新加回来，可能会生成34.jar,这个时候32.jar和34.jar其实
        //内容一模一样，编译时会报类冲突，所以这里我们坐下检查，删除多余的jar文件
        if (isIncrement) {
            jarMap.each {
                jarFiles.add(it.value)
            }
            DirectoryUtils.deleteReduantJar(jaroutDir, jarFiles)
        }
    }
}