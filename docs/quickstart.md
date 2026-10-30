# Quickstart

*A praetor took office with his edict already written. At the start of his year it went up on the album, a
whitened board in the Forum, and every suit brought before him that year went by it. Translations here are the
same: written into the build, so that the running program never goes looking for them.*

## 1. Apply the Plugin and Add the Runtime

**Kotlin JVM:**

```kotlin
plugins {
    kotlin("jvm") version "2.4.0"
    id("io.github.octavius-framework.i18n") version "1.1.1"
}

dependencies {
    implementation("io.github.octavius-framework:i18n-core:1.1.1")
}
```

**Kotlin Multiplatform:**

```kotlin
plugins {
    kotlin("multiplatform") version "2.4.0"
    id("io.github.octavius-framework.i18n") version "1.1.1"
}

kotlin {
    jvm()
    // …and the other targets

    sourceSets {
        commonMain.dependencies {
            implementation("io.github.octavius-framework:i18n-core:1.1.1")
        }
    }
}
```

The plugin comes from the Gradle Plugin Portal and the runtime from Maven Central, both default repositories. The
plugin does not add the runtime by itself; the generated code calls it, so without `i18n-core` it does not
compile. The two are released together, so use one version for both.

## 2. Declare a Generator

```kotlin
octaviusI18n {
    generators {
        create("main") {
            targetPackage = "com.example.i18n"
        }
    }
}
```

Each generator produces one object. All four options are optional:

| Option             | Default                                      | What it decides                                                                                                      |
|:-------------------|:---------------------------------------------|:---------------------------------------------------------------------------------------------------------------------|
| `targetPackage`    | `io.github.octaviusframework.i18n.generated` | The package the object is generated in.                                                                              |
| `objectName`       | `Tr`                                         | The object's name. Two generators writing to one package need different names.                                       |
| `fallbackLanguage` | `en`                                         | The language used when the current one has no file. See [Choosing the Language](choosing-the-language.md).           |
| `sourceProject`    | the project the plugin is applied to         | Where the JSON is read from: that project and all of its subprojects. See [Translation Files](translation-files.md). |

## 3. Write the Translations

`src/main/i18n/pl.json` (in a multiplatform project, `src/commonMain/i18n/pl.json`):

```json
{
  "Legion": {
    "Orders": {
      "march": "Marsz!",
      "halt": "Stój!"
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

`src/main/i18n/en.json`:

```json
{
  "Legion": {
    "Orders": {
      "march": "March!",
      "halt": "Halt!"
    },
    "Units": {
      "cohort": {
        "_one":   "{0} cohort",
        "_other": "{0} cohorts"
      }
    }
  }
}
```

The file name is the language. Nested objects become nested Kotlin objects, and `cohort` is a plural key — see
[Plurals](plurals.md).

## 4. Build and Call

There is nothing to run by hand: compiling the project runs the generator first.

```kotlin
import com.example.i18n.Tr
import io.github.octaviusframework.i18n.core.OctaviusI18n

OctaviusI18n.currentLanguage = "pl"
Tr.Legion.Orders.march()      // "Marsz!"
Tr.Legion.Units.cohort(1)     // "1 kohorta"
Tr.Legion.Units.cohort(3)     // "3 kohorty"
Tr.Legion.Units.cohort(17)    // "17 kohort"

OctaviusI18n.currentLanguage = "en"
Tr.Legion.Orders.march()      // "March!"
Tr.Legion.Units.cohort(17)    // "17 cohorts"
```

The current language starts as `"en"`.

## What the Build Does

Each generator is a task named `generateI18nAccessors` followed by the generator's name with its first letter
capitalised — `generateI18nAccessorsMain` above. It writes to `build/generated/i18n/<generator name>` and clears
that directory before every run, so a renamed object or a dropped language leaves nothing behind.

Before writing anything, it checks the files against each other. A key that one language lacks, or that is a
string in one language and a plural in another, stops the build with a list of every such problem — see
[Translation Files](translation-files.md#one-key-every-language).

The directory is added to the `main` source set of a Kotlin JVM project, or to `commonMain` of a Kotlin
Multiplatform one. A project with neither Kotlin plugin still gets the task, but nothing compiles what it writes.

## Next Steps

- [Translation Files](translation-files.md) — which files a generator reads, naming, placeholders, modules
- [Plurals](plurals.md) — how a form is chosen, and a rule for a language that has none built in
- [Choosing the Language](choosing-the-language.md) — one language for the whole program, and the fallback
