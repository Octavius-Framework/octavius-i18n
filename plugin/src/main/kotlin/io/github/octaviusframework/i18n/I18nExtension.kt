package io.github.octaviusframework.i18n

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration block for a single translation generator.
 *
 * @property name The name of this generator configuration block.
 */
abstract class I18nGeneratorConfig @Inject constructor(val name: String, objects: ObjectFactory) {
    /**
     * Target package where the generated code should be placed.
     * Default: `io.github.octaviusframework.i18n.generated`
     */
    val targetPackage: Property<String> = objects.property(String::class.java).convention("io.github.octaviusframework.i18n.generated")
    
    /**
     * The name of the generated Kotlin object holding the translations.
     * Default: `Tr`
     */
    val objectName: Property<String> = objects.property(String::class.java).convention("Tr")
    
    /**
     * The Gradle project containing the source translation JSON files.
     * If not specified, defaults to the root project.
     */
    val sourceProject: Property<Project> = objects.property(Project::class.java)
    
    /**
     * The fallback language code to use when a translation is missing in the current language.
     * Default: `en`
     */
    val fallbackLanguage: Property<String> = objects.property(String::class.java).convention("en")
}

/**
 * Main Gradle extension for configuring the Octavius I18n plugin.
 */
abstract class OctaviusI18nExtension @Inject constructor(objects: ObjectFactory) {
    /**
     * Container of all generator configurations defined in the build script.
     * You can define multiple generator blocks inside `octaviusI18n`.
     */
    val generators: NamedDomainObjectContainer<I18nGeneratorConfig> = objects.domainObjectContainer(I18nGeneratorConfig::class.java)
}
