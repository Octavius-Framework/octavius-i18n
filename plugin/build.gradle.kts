plugins {
    `kotlin-dsl`
    `maven-publish`
}

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.gson)
}

gradlePlugin {
    plugins {
        create("i18n") {
            id = "io.github.octaviusframework.i18n"
            implementationClass = "io.github.octaviusframework.i18n.I18nPlugin"
        }
    }
}