package com.jianglei.asmplugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class MethodTracePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        def extension = project.getExtensions().findByType(AppExtension.class)
        def isForApplication = true
        if (extension == null) {
            //说明当前使用在library中
            extension = project.getExtensions().findByType(LibraryExtension.class)
            isForApplication = false
        }
        extension.registerTransform(new MethodTraceTransform(project,isForApplication))

    }
}