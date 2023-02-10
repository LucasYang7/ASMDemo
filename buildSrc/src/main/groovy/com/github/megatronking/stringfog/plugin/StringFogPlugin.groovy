package com.github.megatronking.stringfog.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.github.megatronking.stringfog.plugin.utils.Log
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
/**
 * The plugin defines some tasks.
 *
 * @author Megatron King
 * @since 2017/3/6 19:43
 */

class StringFogPlugin implements Plugin<Project> {

    // Gradle插件名
    private static final String PLUGIN_NAME = 'stringfog'

    @Override
    void apply(Project project) {
        // 创建stringfog插件，并且添加相关配置
        project.extensions.create(PLUGIN_NAME, StringFogExtension)

        def android = project.extensions.android
        if (android instanceof AppExtension) {
            applyApplication(project, android)
        }
        if (android instanceof LibraryExtension) {
            applyLibrary(project, android)
        }

        project.afterEvaluate {
            Log.setDebug(project.stringfog.debug)
        }
    }

    static void applyApplication(Project project, def android) {
        // 注册Gradle Transform
        android.registerTransform(new StringFogTransformForApplication(project, android.applicationVariants))
        // throw an exception in instant run mode
        android.applicationVariants.all { variant ->
            def variantName = variant.name.capitalize()
            try {
                def instantRunTask = project.tasks.getByName("transformClassesWithInstantRunFor${variantName}")
                if (instantRunTask) {
                    throw new GradleException(
                            "StringFog does not support instant run mode, please trigger build"
                                    + " by assemble${variantName} or disable instant run"
                                    + " in 'File->Settings...'."
                    )
                }
            } catch (UnknownTaskException e) {
                // Not in instant run mode, continue.
            }
        }
    }

    static void applyLibrary(Project project, def android) {
        android.registerTransform(new StringFogTransformForLibrary(project, android.libraryVariants))
    }

}