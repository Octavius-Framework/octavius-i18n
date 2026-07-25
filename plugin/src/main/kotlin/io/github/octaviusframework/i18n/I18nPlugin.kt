package io.github.octaviusframework.i18n

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class I18nPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("octaviusI18n", OctaviusI18nExtension::class.java)

        target.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            val ext = target.extensions.getByType(KotlinMultiplatformExtension::class.java)
            ext.sourceSets.getByName("commonMain").kotlin.srcDir(target.layout.buildDirectory.dir("generated/kotlin/commonMain"))
        }
        target.plugins.withId("org.jetbrains.kotlin.jvm") {
            val ext = target.extensions.getByType(KotlinJvmProjectExtension::class.java)
            ext.sourceSets.getByName("main").kotlin.srcDir(target.layout.buildDirectory.dir("generated/kotlin/commonMain"))
        }

        target.afterEvaluate {
            val allGenTasks = mutableListOf<org.gradle.api.tasks.TaskProvider<*>>()
            
            extension.generators.forEach { config ->
                val tp = target.registerGenerateI18nAccessorsTask(
                    targetProject = target,
                    sourceProject = config.sourceProject.getOrElse(target.rootProject),
                    targetPackage = config.targetPackage.get(),
                    objectName = config.objectName.get()
                )
                allGenTasks.add(tp)
            }

            target.tasks.configureEach {
                if (name.startsWith("compileKotlin") || name.endsWith("SourcesJar") || name == "sourcesJar" || name.startsWith("ksp")) {
                    allGenTasks.forEach { dependsOn(it) }
                }
            }
        }
    }
}
