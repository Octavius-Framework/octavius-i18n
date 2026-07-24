import io.github.octaviusframework.i18n.registerGenerateI18nAccessorsTask

plugins {
    kotlin("jvm") version "2.4.0"
    id("io.github.octaviusframework.i18n")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.octavius-framework:core:0.9.5")
    testImplementation(kotlin("test"))
}

registerGenerateI18nAccessorsTask(
    coreProject = project,
    sourceProject = project,
    targetPackage = "org.example.i18n",
    objectName = "Tr"
)

kotlin {
    sourceSets.main {
        kotlin.srcDir(layout.buildDirectory.dir("generated/kotlin/commonMain"))
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateI18nAccessors")
}

tasks.test {
    useJUnitPlatform()
}
