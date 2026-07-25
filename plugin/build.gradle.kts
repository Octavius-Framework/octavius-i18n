plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.3.0"
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
    implementation(libs.kotlinx.serialization.json)
    compileOnly(kotlin("gradle-plugin", libs.versions.kotlin.get()))
}

gradlePlugin {
    website.set("https://github.com/octavius-framework/octavius-i18n")
    vcsUrl.set("https://github.com/octavius-framework/octavius-i18n.git")
    
    plugins {
        create("i18n") {
            id = "io.github.octaviusframework.i18n"
            displayName = "Octavius I18n"
            description = "A type-safe, code-generated localization plugin for Kotlin."
            tags.set(listOf("kotlin", "i18n", "localization", "kmp", "multiplatform"))
            implementationClass = "io.github.octaviusframework.i18n.I18nPlugin"
        }
    }
}
