plugins {
    alias(libs.plugins.kotlinMultiplatform)
    `maven-publish`
}

kotlin {
    jvmToolchain(21)

    jvm()
}

repositories {
    mavenCentral()
}
