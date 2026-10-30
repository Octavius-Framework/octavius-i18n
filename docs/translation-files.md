# Translation Files

*Schoolbooks for learning Latin and Greek came down under the name of Dositheus, a grammarian who did not write
them. Their word lists set the two languages side by side, and some were arranged by subject rather than by
letter: the gods in one place, the heavens in another. A word was found under its heading, and the heading was
the same in both languages.*

A translation file is a JSON object named after its language. Its nesting is the shape of the generated object,
and it has to be the same shape in every language.

Contents:
* [Which files a generator reads](#which-files-a-generator-reads)
* [The language of a file](#the-language-of-a-file)
* [From JSON to Kotlin](#from-json-to-kotlin)
* [Placeholders](#placeholders)
* [One key, every language](#one-key-every-language)

## Which files a generator reads

Every file matching `src/**/i18n/*.json` in the generator's `sourceProject` and in all of its subprojects. That
is `src/main/i18n/` and `src/commonMain/i18n/`, but test source sets too: `src/test/i18n/` goes into the same
object as `src/main/i18n/`.

Without `sourceProject`, the generator reads the project the plugin is applied to, with its subprojects. A plugin
applied in the root project therefore reads the whole build.

**A generator always compiles into the project that declares it**; `sourceProject` decides only where it reads
from. A second generator gives one module's keys an object of their own:

```kotlin
octaviusI18n {
    generators {
        create("main") {
            targetPackage = "com.example.i18n"
        }
        create("camp") {
            sourceProject = project(":camp")
            targetPackage = "com.example.camp.i18n"
            objectName = "CampTr"
        }
    }
}
```

Declared in the root project, `Tr` holds the keys of the whole build, `:camp`'s among them, and `CampTr` holds
only `:camp`'s. Both are compiled into the root project. For an object that `:camp` itself can call, apply the
plugin in `:camp`.

## The language of a file

The file name without `.json` is the language code: `pl.json` is `"pl"`, `pt-BR.json` is `"pt-BR"`. It is matched
against `OctaviusI18n.currentLanguage` exactly, case included.

All files of one language are merged into one tree, object into object, so `Legion.Orders` can come from one
module and `Legion.Units` from another. The same key in two files of one language is not an error: one of them
wins, without a warning. Keep each key in one file.

A file that does not parse as JSON is logged as an error and skipped, and the build goes on without it. If it was
a language's only file, that language has no file at all.

## From JSON to Kotlin

An object becomes a Kotlin `object`, a string becomes a function returning `String`, and an object made only of
plural forms becomes a plural function — see [Plurals](plurals.md).

```json
{
  "Legion": {
    "Orders": {
      "march": "Marsz!"
    },
    "siege_engines": {
      "heavy-ballista": "Ciężka balista"
    },
    "Gates": {
      "in": "Wejście"
    }
  }
}
```

```kotlin
Tr.Legion.Orders.march()                 // "Marsz!"
Tr.Legion.SiegeEngines.heavyBallista()   // "Ciężka balista"
Tr.Legion.Gates.`in`()                   // "Wejście"
```

Objects are named in PascalCase and functions in camelCase, with `_` and `-` separating words; every other letter
is kept as written. A Kotlin keyword is escaped with backticks.

Accessors are functions, not properties. Each call reads the current language, so the same line returns another
language after a switch.

## Placeholders

`{0}`, `{1}`, … in a string make its function take one `Any` parameter per index, up to the highest one used.
Each argument is inserted with `toString()`.

```json
"greeting": "Ave, {0}! Witaj w legionie {1}."
```

```kotlin
Tr.Legion.greeting("Marcus", "IX")   // "Ave, Marcus! Witaj w legionie IX."
```

- A gap still counts: `"{0} i {2}"` takes three arguments, and the second one is never shown.
- Only a number in braces is a placeholder. `{name}` is text and stays as written; a numbered one cannot be
  escaped, so a translation cannot show a literal `{0}`.
- An argument is inserted as it is. An argument that itself contains `{1}` is not expanded.

## One key, every language

Accessors are generated from the union of all languages: a key present in any file gets a function. In a
language whose file lacks it, the function returns the key's path:

```kotlin
// "retreat" is in pl.json only
OctaviusI18n.currentLanguage = "en"
Tr.Legion.Orders.retreat()   // "Legion.Orders.retreat"
```

A translation missing from one language therefore shows up as its path rather than failing the build. It is not
taken from the fallback language either; see
[Choosing the Language](choosing-the-language.md#a-key-missing-from-a-language).

Because one function serves every language, **a key must have the same shape in each**: a string everywhere or a
plural everywhere, with the same placeholders. Nothing checks this. The signature comes from one of the files,
and a language that disagrees with it renders wrong.
