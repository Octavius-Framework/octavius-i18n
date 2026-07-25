# Octavius I18n

> *"Divide et impera"* — and so, translations are divided by module, ruled by type safety.

A type-safe, code-generated localization plugin. Write your translations once in JSON, run the generator, and let the compiler catch every missing key — just as a Roman scribe would catch every missing seal.

---

## Features

- **Type-safe accessors** — no string keys, no runtime typos, no mercy for mistakes
- **Build-time code generation** — the `Tr` object is forged at compile time, not at runtime
- **Plural forms** — `one`, `two`, `few`, `many`, `other`, `zero` via pluggable `PluralRule`
- **Per-module translations** — each module carries its own `i18n/<lang>.json`
- **Runtime language switching** — change `OctaviusI18n.currentLanguage` and the legion regroups instantly
- **Extensible plural rules** — add your own language rules by mutating `OctaviusI18n.pluralRules`

---

## Installation

```kotlin
// build.gradle.kts
plugins {
    id("io.github.octaviusframework.i18n") version "1.0.0"
}

dependencies {
    // The core runtime library
    implementation("io.github.octaviusframework:i18n-core:1.0.0")
}
```

---

## Quick Start

### 1. Configure the Generator

Define your generators using the clean DSL. You can create multiple generators if you want to split translations across features.

```kotlin
octaviusI18n {
    generators {
        create("main") {
            targetPackage = "com.yourcompany.i18n"
            objectName = "Tr"            // Optional, defaults to "Tr"
            fallbackLanguage = "en"      // Optional, defaults to "en"
            // sourceProject = project(":other-module")  // Optional, defaults to rootProject
        }
    }
}
```

### 2. Define translations

Create `i18n/pl.json` in your module's source (e.g. `src/main/i18n/pl.json` or `src/commonMain/i18n/pl.json`):

```json
{
  "Legion": {
    "Orders": {
      "march": "Marsz!",
      "halt": "Stój!"
    },
    "Units": {
      "cohort": {
        "_one":   "1 kohorta",
        "_few":   "{0} kohorty",
        "_many":  "{0} kohort",
        "_other": "{0} kohort"
      }
    }
  }
}
```

### 3. Generate & Use (Fully Automated!)

You don't need to manually run any tasks. The plugin **automatically hooks into the Kotlin compilation lifecycle**. Just build your project:

```bash
./gradlew build
```

The generator produces the `Tr` object — your stone tablet of localized strings, carved once, referenced everywhere. Generated sources land in `build/generated/kotlin/commonMain`.

```kotlin
import com.yourcompany.i18n.Tr

Tr.Legion.Orders.march()            // → "Marsz!"
Tr.Legion.Orders.halt()             // → "Stój!"
Tr.Legion.Units.cohort(1)           // → "1 kohorta"
Tr.Legion.Units.cohort(3)           // → "3 kohorty"
Tr.Legion.Units.cohort(17)          // → "17 kohort"
Tr.Legion.Units.cohort(1.5)         // → "1.5 kohort"  (Double overload)

// Switch language globally across all modules
OctaviusI18n.currentLanguage = "en"
Tr.Legion.Orders.march()            // → "March!"
```

---

## Plural Forms

Plural selection is handled by `PluralRule` implementations registered in `OctaviusI18n.pluralRules`. English and Polish rules are built in. You can add any language by implementing the `PluralRule` interface:

```kotlin
OctaviusI18n.pluralRules["uk"] = object : PluralRule {
    override fun selectForm(count: Int) = when {
        count % 10 == 1 && count % 100 != 11 -> "one"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "few"
        else -> "many"
    }
    override fun selectForm(count: Double) = "other"
}
```

Available plural keys for use in JSON:

| Key      | Meaning                                                                   | Example (Polish) |
|----------|---------------------------------------------------------------------------|------------------|
| `_zero`  | Exactly zero items (some languages treat 0 specially)                     | —                |
| `_one`   | Exactly one item (singular)                                               | *1 kohorta*      |
| `_two`   | Exactly two items (some languages, e.g. Arabic, Welsh)                    | —                |
| `_few`   | Small count, per language rules (e.g. 2–4 in Polish)                      | *3 kohorty*      |
| `_many`  | Larger count, per language rules (e.g. 5+ in Polish)                      | *17 kohort*      |
| `_other` | Catch-all fallback — always included when present, raw key used otherwise | *kohort*         |

You don't need to define every form — only those your language uses. If `_other` is absent, the raw translation key is returned as a last resort.

### Which forms does my language need?

Different languages conscript different plural forms into service:

| Language | Forms used                                         |
|----------|----------------------------------------------------|
| English  | `_one`, `_other`                                   |
| Polish   | `_one`, `_few`, `_many`, `_other`                  |
| Russian  | `_one`, `_few`, `_many`, `_other`                  |
| Arabic   | `_zero`, `_one`, `_two`, `_few`, `_many`, `_other` |
| Japanese | `_other` (no plural distinction)                   |
| Czech    | `_one`, `_few`, `_many`, `_other`                  |

Consult the [CLDR plural rules table](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html) for your language.

### Example: English

```json
{
  "Legion": {
    "Units": {
      "cohort": {
        "_one":   "1 cohort",
        "_other": "{0} cohorts"
      }
    }
  }
}
```

### Example: Arabic (all six forms)

```json
{
  "Legion": {
    "Units": {
      "cohort": {
        "_zero":  "لا توجد كتائب",
        "_one":   "كتيبة واحدة",
        "_two":   "كتيبتان",
        "_few":   "{0} كتائب",
        "_many":  "{0} كتيبة",
        "_other": "{0} كتيبة"
      }
    }
  }
}
```

---

## JSON Structure

Translations are organized as nested objects. The nesting maps directly to the generated `Tr` accessor hierarchy.

```
src/
└── main/
    └── i18n/
        ├── pl.json
        └── en.json
```

Keys may contain:

- Plain strings: `"march": "Marsz!"`
- Strings with indexed placeholders: `"greeting": "Witaj, {0}! Jesteś w {1} legionie."`
- Plural objects with form keys (see above)

Placeholders use `{0}`, `{1}` syntax and are passed as ordered parameters to the generated function:

```kotlin
// JSON: "greeting": "Witaj, {0}! Zwerbowano cię do {1} legionu."
Tr.Legion.greeting("Kacper", 9)
// → "Witaj, Kacper! Zwerbowano cię do 9 legionu."
```

### Multiple Plurals in One Sentence (Composition)

If your sentence depends on more than one pluralized number (e.g. *"5 men bought 2 apples"*), standard JSON localization requires splitting the sentence into smaller, manageable parts. This prevents a combinatorial explosion of keys and is exactly how Android string resources handle it natively.

```json
{
  "Legion": {
    "men": { "_one": "1 mężczyzna", "_other": "{0} mężczyzn" },
    "apples": { "_one": "1 jabłko", "_other": "{0} jabłek" },
    "purchased": "{0} kupiło {1}"
  }
}
```

Compose them in code:
```kotlin
Tr.Legion.purchased( Tr.Legion.men(5), Tr.Legion.apples(2) )
// → "5 mężczyzn kupiło 2 jabłka"
```

---

## Code Generation

The plugin automatically scans all `i18n/*.json` files in the configured source project (all subprojects included) and generates the `Tr` object directly into the compilation pipeline.

### Bulletproof Union of Keys
If a translator forgets to add a key to `de.json` but a developer added it to `en.json`, the build **will not fail** and the accessor will still be generated. The generator merges all keys from all language files into a union. If the missing translation is requested at runtime, the library gracefully falls back to displaying the raw key (e.g. `"Legion.Orders.march"`).

The generated code delegates all formatting to the `OctaviusI18n` engine, guaranteeing seamless integration across massive multi-module projects without duplicating logic. *Caesar didn't write his own dispatches either.*

---

## Runtime Language Switching

```kotlin
OctaviusI18n.currentLanguage = "en"
```

The change takes effect immediately for all subsequent calls. Thread safety is your province to govern — neither `currentLanguage` nor `pluralRules` impose synchronization.

---

## Supported Languages

Any language with a `<lang>.json` file inside an `i18n` folder is automatically enrolled. The language code matches the filename suffix (`pl`, `en`, `de`, `ar`, etc.). If no `PluralRule` is registered for a language, the library falls back to the English rule, then to `"other"`.

---

## License

Apache 2.0 — *use it freely, as Rome used its roads.*
```
