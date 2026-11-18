rootProject.name = "octavius-i18n"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

// Where Gradle downloads the JDK 21 the modules' toolchains ask for, when the machine has none
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("plugin", "core")

project(":plugin").name = "i18n-plugin"
project(":core").name = "i18n-core"