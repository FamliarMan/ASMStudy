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
        //暂时简单复制过去
        FileUtils.copyFile(inputFile, outputFile)
    }

    public static void traceJar(File jar, File outputFile) {
        //暂时简单复制过去
        FileUtils.copyFile(jar, outputFile)

    }
}