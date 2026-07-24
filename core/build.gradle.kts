plugins {
    alias(libs.plugins.kotlinMultiplatform)
    `maven-publish`
}

kotlin {
    jvmToolchain(25)

    jvm()
}

repositories {
    mavenCentral()
}
