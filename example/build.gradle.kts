
plugins {
    kotlin("jvm") version "2.4.0"
    id("io.github.octaviusframework.i18n")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.octavius-framework:i18n-core:0.9.8")
    testImplementation(kotlin("test"))
}

octaviusI18n {
    generators {
        create("main") {
            targetPackage = "org.example.i18n"
            objectName = "Tr"
        }
        create("feature") {
            sourceProject = project(":feature")
            targetPackage = "org.example.feature.i18n"
            objectName = "FeatureTr"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
