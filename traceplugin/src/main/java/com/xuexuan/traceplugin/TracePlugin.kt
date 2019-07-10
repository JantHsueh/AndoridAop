package com.xuexuan.traceplugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project



class TracePlugin :  Plugin<Project> {


    override fun apply(project: Project) {
        val android = project.extensions.getByType(AppExtension::class.java)
        android.registerTransform(LogTransform())
    }

}