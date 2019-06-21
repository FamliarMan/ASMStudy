package com.jianglei.asmplugin

import com.android.build.api.transform.JarInput
import org.apache.commons.io.FileUtils

class MethodTraceUtils {

    /**
     * 对输入的文件进行处理，并将处理后的文件输出到outputFile
     * @param inputFile 输入文件
     * @param outputFile 处理后的输出文件
     */
    public static void traceFile(File inputFile, File outputFile) {
        //暂时简单复制过去
        FileUtils.copyFile(inputFile, outputFile)
    }

    public static void traceJar(JarInput jarInput, File outputFile) {

        def jar = jarInput.file
        LogUtils.i("正在处理jar:" + jarInput.name)
        //jar包解压的临时位置
        def tmpDir = outputFile.parentFile.absolutePath + File.separator + outputFile
                .name.replace(".jar", File.separator)
        def tmpFile = new File(tmpDir)
        tmpFile.mkdirs()
        //先解压缩到临时目录
        MyZipUtils.unzip(jar.absolutePath, tmpFile.absolutePath)
        //收集解压缩后的所有文件
        def allFiles = new ArrayList()
        collectFiles(tmpFile, allFiles)
        allFiles.each {
            if (isNeedTraceClass(it)) {
                //将处理后的文件命名成原名称-new形式
                def tracedFile = new File(tmpFile.absolutePath + "-new")
                traceFile(it, tracedFile)
                //处理完后用新的文件替换原有文件
                it.delete()
                tracedFile.renameTo(it)
            }
        }
        MyZipUtils.zip(tmpFile.absolutePath, outputFile.absolutePath)
        tmpFile.deleteDir()


    }

    private static void collectFiles(File dir, List<File> res) {
        if (!dir.isDirectory()) {
            return
        }
        def children = dir.listFiles()
        if (children == null || children.size() == 0) {
            return
        }
        for (File file : children) {
            if (!file.isDirectory()) {
                res.add(file)
            } else {
                collectFiles(file, res)
            }
        }
    }


    /**
     * 判断一个文件是否需要插入方法追踪代码
     * @param file 要判断的文件
     * @return 需要返回true
     */
    public static boolean isNeedTraceClass(File file) {
        def name = file.name
        if (!name.endsWith(".class") || name.startsWith("R.class") || name.startsWith('R$')) {
            //R类文件不追踪，非class文件不追踪
            return false
        }
        return true

    }
}