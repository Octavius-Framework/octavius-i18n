package io.github.octaviusframework.i18n

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class I18nGeneratorConfig @Inject constructor(val name: String, objects: ObjectFactory) {
    val targetPackage: Property<String> = objects.property(String::class.java).convention("io.github.octaviusframework.i18n.generated")
    val objectName: Property<String> = objects.property(String::class.java).convention("Tr")
    val sourceProject: Property<Project> = objects.property(Project::class.java)
    val fallbackLanguage: Property<String> = objects.property(String::class.java).convention("en")
}

abstract class OctaviusI18nExtension @Inject constructor(objects: ObjectFactory) {
    val generators: NamedDomainObjectContainer<I18nGeneratorConfig> = objects.domainObjectContainer(I18nGeneratorConfig::class.java)
}
