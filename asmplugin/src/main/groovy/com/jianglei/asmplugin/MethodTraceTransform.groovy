package com.jianglei.asmplugin


import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
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

        def rootOutDirPath = project.buildDir.absolutePath + File.separator + "asmout"
        transformInvocation.inputs
                .each { input ->
            input.directoryInputs.each { directoryInput ->

                LogUtils.i("----------------------changed files-----------------------------------")
                directoryInput.changedFiles.each {
                    LogUtils.i("change files:" + it.key.absolutePath + "  " + it.value.toString())
                }
                def rootInputPath = directoryInput.file.absolutePath
                def allFiles = DirectoryUtils.getAllFiles(directoryInput.file)
                for (File file : allFiles) {
                    def outputFullPath = file.absolutePath.replace(rootInputPath, rootOutDirPath)
                    def outputFile = new File(outputFullPath)
                    if (!outputFile.parentFile.exists()) {
                        outputFile.parentFile.mkdirs()
                    }
                    FileUtils.copyFile(file, new File(outputFullPath))
                    LogUtils.i("directory out:" + outputFullPath)
                }

            }
            input.jarInputs.each { jarInput ->

                //重命名jar文件，否则同目录copyFile会冲突
                def jarName = jarInput.name
                def md5Name = DigestUtils.md5Hex(jarInput.file.absolutePath)
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4)
                }
                def sb = new StringBuilder()
                def outputFullPath = sb.append(rootOutDirPath).append(File.separator).append(jarName)
                        .append("_")
                        .append(md5Name)
                        .append(".jar")
                        .toString()
                FileUtils.copyFile(jarInput.file, new File(outputFullPath))
                LogUtils.i("jar out:" + outputFullPath)
            }
        }
    }
}