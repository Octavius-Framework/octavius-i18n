plugins {
    alias(libs.plugins.kotlinJvm)
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "2.1.1"
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    compileOnly(gradleApi())
    implementation(libs.kotlinx.serialization.json)
    compileOnly(kotlin("gradle-plugin", libs.versions.kotlin.get()))
}

gradlePlugin {
    website.set("https://github.com/octavius-framework/octavius-i18n")
    vcsUrl.set("https://github.com/octavius-framework/octavius-i18n.git")
    
    plugins {
        create("i18n") {
            id = "io.github.octavius-framework.i18n"
            displayName = "Octavius I18n"
            description = "A type-safe, code-generated localization plugin. Write your translations once in JSON, run the generator, and let the compiler catch every missing key — just as a Roman scribe would catch every missing seal."
            tags.set(listOf("kotlin", "i18n", "localization", "kmp", "multiplatform"))
            implementationClass = "io.github.octaviusframework.i18n.I18nPlugin"
        }
    }
}
