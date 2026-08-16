import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.publish) apply false
}

allprojects {
    group = "io.github.octavius-framework"
    version = "1.0.1"
}

dokka {
    moduleName.set("Octavius I18n")

    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
    }
}

dependencies {
    dokka(project(":i18n-core"))
    dokka(project(":i18n-plugin"))
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")

    extensions.configure<DokkaExtension> {
        moduleName.set(name)

        dokkaSourceSets.configureEach {
            documentedVisibilities.set(
                setOf(
                    VisibilityModifier.Public,
                    VisibilityModifier.Protected,
                    VisibilityModifier.Internal
                )
            )
            skipEmptyPackages.set(true)
        }
    }

    val publishedProjects = listOf("i18n-core", "i18n-plugin")

    if (publishedProjects.contains(project.name)) {
        apply(plugin = "com.vanniktech.maven.publish")

        extensions.configure<MavenPublishBaseExtension> {
            coordinates(group.toString(), project.name, version.toString())

            pom {
                name.set("Octavius I18n - ${project.name}")
                description.set("A type-safe, code-generated localization plugin. Write your translations once in JSON, run the generator, and let the compiler catch every missing key — just as a Roman scribe would catch every missing seal.")
                url.set("https://github.com/Octavius-Framework/octavius-i18n")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("PolskiAnonim")
                        name.set("PolskiAnonim")
                        email.set("115878440+PolskiAnonim@users.noreply.github.com")
                        organization.set("Octavius Framework")
                        organizationUrl.set("https://github.com/Octavius-Framework")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/Octavius-Framework/octavius-i18n.git")
                    developerConnection.set("scm:git:ssh://github.com/Octavius-Framework/octavius-i18n.git")
                    url.set("https://github.com/Octavius-Framework/octavius-i18n")
                }
            }

            publishToMavenCentral()

            val isSigningKeyPresent = project.hasProperty("signingInMemoryKey") ||
                project.hasProperty("signingKey") ||
                System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null
            if (isSigningKeyPresent) {
                signAllPublications()
            }
        }

        if (project.name == "i18n-plugin") {
            // GradlePublishPlugin() (required for com.gradle.plugin-publish compatibility)
            // hardcodes JavadocJar.Javadoc(), i.e. Gradle's plain `javadoc` task - which is
            // empty for a Kotlin-only module. Point the javadocJar task it creates at the
            // Dokka output instead. Uses configureEach (not afterEvaluate) since that task is
            // only registered once com.gradle.plugin-publish itself evaluates - by its own
            // build.gradle.kts, which runs after this subprojects{} block.
            tasks.withType<Jar>().configureEach {
                if (name == "javadocJar") {
                    from(tasks.named("dokkaGeneratePublicationHtml"))
                }
            }
        }
    }
}
