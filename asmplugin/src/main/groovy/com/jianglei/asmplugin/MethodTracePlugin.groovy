package com.jianglei.asmplugin

import com.android.build.gradle.AppExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class MethodTracePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        //确保只能在含有application的build.gradle文件中引入
        if (!project.plugins.hasPlugin('com.android.application')) {
            throw new GradleException('Android Application plugin required')
        }
        project.getExtensions().findByType(AppExtension.class)
                .registerTransform(new MethodTraceTransform(project))

    }
}