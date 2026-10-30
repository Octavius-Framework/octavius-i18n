# Octavius I18n

[![Maven Central](https://img.shields.io/maven-central/v/io.github.octavius-framework/i18n-core?label=Maven%20Central)](https://central.sonatype.com/namespace/io.github.octavius-framework)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.octavius-framework.i18n?label=Gradle%20Plugin)](https://plugins.gradle.org/plugin/io.github.octavius-framework.i18n)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

*"Divide et impera" — translations divided by module, ruled by type safety.*

A Gradle plugin that turns `i18n/<lang>.json` files into a Kotlin object with one function per key, and a small
multiplatform runtime that the generated code calls. You write `Tr.Legion.Orders.march()` rather than
`t("Legion.Orders.march")`, so a key that no file has is a compile error rather than a string that turns up
untranslated in production.

The translations are compiled in. Nothing is read at runtime and there are no resource files to find, so the
same generated code runs on every target the runtime is built for.

## What is here

| Artifact                         | What it is                                                                                                                                                                         |
|:---------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **`i18n-plugin`** — *the scribe* | The Gradle plugin `io.github.octavius-framework.i18n`. Reads every `i18n/<lang>.json` of a project and its subprojects, generates the accessor object, and adds it to compilation. |
| **`i18n-core`** — *the tablet*   | The runtime the generated code calls: the current language, the plural rules, placeholders. Kotlin Multiplatform: JVM, JS, Wasm (JS and WASI), Linux, Windows, macOS, iOS.         |

The scribe copies the text out once, at build time; the tablet is what gets read afterwards, in whichever
language is current.

## Coordinates

Both are released together on the same version:

```kotlin
plugins {
    id("io.github.octavius-framework.i18n") version "1.1.1"
}

dependencies {
    implementation("io.github.octavius-framework:i18n-core:1.1.1")
}
```

The plugin does not add the runtime by itself, and the generated code does not compile without it. In a Kotlin
Multiplatform project the runtime goes into `commonMain` instead — see the [Quickstart](docs/quickstart.md).

## At a glance

`src/main/i18n/pl.json`:

```json
{
  "Legion": {
    "Orders": {
      "march": "Marsz!"
    },
    "Units": {
      "cohort": {
        "_one":   "{0} kohorta",
        "_few":   "{0} kohorty",
        "_many":  "{0} kohort",
        "_other": "{0} kohorty"
      }
    }
  }
}
```

```kotlin
OctaviusI18n.currentLanguage = "pl"

Tr.Legion.Orders.march()      // "Marsz!"
Tr.Legion.Units.cohort(3)     // "3 kohorty"
Tr.Legion.Units.cohort(17)    // "17 kohort"
```

## Requirements

- **Java 21+** — the plugin and the JVM runtime are compiled for 21, so Gradle has to run on 21 or newer, and so
  does a JVM application.
- **The Kotlin JVM or Kotlin Multiplatform Gradle plugin** in the project that applies this one. The generated
  sources go into `main` or `commonMain`; with any other setup the generation task still exists, but nothing
  compiles what it writes.

## Documentation

The guides start from the [documentation index](docs/README.md); the [Quickstart](docs/quickstart.md) is where to
begin from an empty project. Every change is recorded in the [CHANGELOG](CHANGELOG.md).

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
