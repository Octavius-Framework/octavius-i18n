package io.github.octaviusframework.i18n

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * The entry point for the Octavius I18n Gradle plugin.
 * 
 * This plugin sets up the `octaviusI18n` extension and dynamically registers
 * code generation tasks for each configured translation generator. 
 * It automatically hooks generated sources into the Kotlin Multiplatform or Kotlin JVM compilation process.
 */
class I18nPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Register the main extension allowing users to configure the plugin in build.gradle.kts
        val extension = target.extensions.create("octaviusI18n", OctaviusI18nExtension::class.java)

        extension.generators.all(object : Action<I18nGeneratorConfig> {
            override fun execute(config: I18nGeneratorConfig) {
                val taskName = "generateI18nAccessors${config.name.replaceFirstChar { it.uppercaseChar() }}"

                val genTask = target.tasks.register(taskName, GenerateI18nTask::class.java, object : Action<GenerateI18nTask> {
                    override fun execute(t: GenerateI18nTask) {
                        t.group = "build"
                        t.description = "Generates type-safe Kotlin accessors for translations."

                        t.targetPackage.set(config.targetPackage)
                        t.objectName.set(config.objectName)
                        t.fallbackLanguage.set(config.fallbackLanguage)
                        t.outputDir.set(target.layout.buildDirectory.dir("generated/kotlin/commonMain"))

                        t.sourceFiles.set(config.sourceProject.orElse(target.rootProject).map { sp ->
                            val fc = target.files()
                            sp.allprojects.forEach { sub ->
                                fc.from(sub.fileTree("src") { it.include("**/i18n/*.json") })
                            }
                            fc
                        })
                    }
                })

                target.plugins.withId("org.jetbrains.kotlin.multiplatform") {
                    val ext = target.extensions.getByType(KotlinMultiplatformExtension::class.java)
                    ext.sourceSets.getByName("commonMain").kotlin.srcDir(genTask)
                }
                target.plugins.withId("org.jetbrains.kotlin.jvm") {
                    val ext = target.extensions.getByType(KotlinJvmProjectExtension::class.java)
                    ext.sourceSets.getByName("main").kotlin.srcDir(genTask)
                }

            }
        })
    }
}
