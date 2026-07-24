plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "io.github.octaviusframework"
version = "0.9.4"

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
}

gradlePlugin {
    plugins {
        create("i18n") {
            id = "io.github.octaviusframework.i18n"
            implementationClass = "io.github.octaviusframework.i18n.I18nPlugin"
        }
    }
}