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

}