rootProject.name = "octavius-i18n"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("plugin", "core")

project(":plugin").name = "i18n-plugin"
project(":core").name = "i18n-core"