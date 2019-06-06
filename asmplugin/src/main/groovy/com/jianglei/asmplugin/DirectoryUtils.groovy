package com.jianglei.asmplugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class DirectoryUtils {

    /**
     * 获取某个目录下的所有文件，递归查找
     * @param dir 需要递归查找所有文件的目录
     * @return 返回含有所有平铺file的list结构，永远不为空
     */
    public static List<File> getAllFiles(File dir) {
        def res = new ArrayList<File>()
        if (!dir.isDirectory()) {
            res.add(dir)
            return res
        }
        def childFiles = dir.listFiles()
        if (childFiles == null || childFiles.length == 0) {
            return res
        }
        for (File file : childFiles) {
            if (file.isDirectory()) {
                res.addAll(getAllFiles(file))
            } else {
                res.add(file)
            }
        }
        return res
    }

    /**
     * 删除某个目录的所有同级目录，包括自己
     * @param dir 要删除的某个目录
     */
    static void deleteSameLevelDirs(String dir) {
        if (dir == null) {
            return
        }
        def childFiles = new File(dir).parentFile.listFiles()
        if (childFiles == null) {
            return
        }
        for (File file : childFiles) {
            if (file.isDirectory()) {
                file.deleteDir()
            }
        }
    }
    /**
     * 判断某个路径的父级目录下是否有目录
     * @param path 要查看的路径
     * @return 有目录返回true，没有返回false
     */
    static boolean hasDir(String path) {
        if (path == null) {
            return false
        }
        def files = new File(path).parentFile.listFiles()
        if (files == null) {
            return false
        }
        for (File file : files) {
            if (file.isDirectory()) {
                return true
            }
        }
        return false

    }

    /**
     * 删除某个路径的同级所有的jar文件
     * @param path 当前路径
     */
    static void deleteAllJars(String path) {
        if (path == null) {
            return
        }
        def files = new File(path).parentFile.listFiles()
        if (files == null) {
            return
        }
        for (File file : files) {
            if (file.name.endsWith(".jar")) {
                file.delete()
            }
        }
    }

    /**
     * 删除某个路径下同级的多余的jar文件
     * @param validFiles 要保留的jar文件
     */
    static void deleteReduantJar(String path, Set<String> validFiles) {
        def file = new File(path)
        if (file.parentFile == null) {
            return
        }
        def files = new File(path).parentFile.listFiles()
        if (files == null) {
            return
        }
        for (File f : files) {
            if (!validFiles.contains(f.absolutePath) && f.absolutePath.endsWith(".jar")) {
                f.delete()
                LogUtils.i("delete jar:"+f.absolutePath)
            }
        }
    }

}