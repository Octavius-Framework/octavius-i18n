import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier

plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.dokka)
    `maven-publish`
    signing
}

allprojects {
    group = "io.github.octavius-framework"
    version = "1.0.0"
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

    plugins.withId("maven-publish") {

        configure<PublishingExtension> {

            publications.withType<MavenPublication>().configureEach {
                val pubName = name

                if (pubName != "pluginMaven" && !pubName.endsWith("PluginMarkerMaven")) {
                    val javadocTask = project.tasks.register<Jar>("${pubName}JavadocJar") {
                        archiveClassifier.set("javadoc")
                        archiveAppendix.set(pubName)
                        from(tasks.named("dokkaGeneratePublicationHtml"))
                    }
                    artifact(javadocTask)
                }

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
            }

            repositories {
                maven {
                    name = "LocalStaging"
                    url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
                }
            }
        }

        project.tasks.withType<Jar>().configureEach {
            if (name == "javadocJar") {
                from(tasks.named("dokkaGeneratePublicationHtml"))
            }
        }

        project.apply(plugin = "signing")
        configure<SigningExtension> {
            val signingKey = System.getenv("OSSRH_GPG_SECRET_KEY")
            val signingPassword = System.getenv("OSSRH_GPG_SECRET_KEY_PASSWORD")

            if (!signingKey.isNullOrBlank()) {
                isRequired = true
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(extensions.getByType<PublishingExtension>().publications)
            } else {
                isRequired = false
            }
        }
    }
}
