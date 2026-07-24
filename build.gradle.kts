plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "io.github.octaviusframework"
version = "0.9.0"

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
        create("translations") {
            id = "io.github.octaviusframework.translations"
            implementationClass = "io.github.octaviusframework.translations.TranslationsPlugin"
        }
    }
}