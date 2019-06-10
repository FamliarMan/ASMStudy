package com.jianglei.asmplugin

import com.android.build.gradle.AppExtension
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

class MethodTraceUtils {

    /**
     * 对输入的文件进行处理，并将处理后的文件输出到outputFile
     * @param inputFile 输入文件
     * @param outputFile 处理后的输出文件
     */
    public static void traceFile(File inputFile, File outputFile) {
        if (isNeedTraceClass(inputFile)) {
            def classWriter = ASMUtils.insertByteCode(inputFile)
            def outStream = new FileOutputStream(outputFile)
            outStream.write(classWriter.toByteArray())
            outStream.close()

        } else {
            FileUtils.copyFile(inputFile, outputFile)
        }
    }

    public static void traceJar(File jar, File outputFile) {
        //暂时简单复制过去
        FileUtils.copyFile(jar, outputFile)

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