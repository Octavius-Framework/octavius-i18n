plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvmToolchain(21)

    jvm()
    js {
        browser()
        nodejs()
    }
    wasmJs {
        browser()
        nodejs()
    }
    wasmWasi {
        nodejs()
    }
    
    linuxX64()
    linuxArm64()
    mingwX64()

    macosArm64()
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()
}

repositories {
    mavenCentral()
}
