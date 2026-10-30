# Translation Files

*Schoolbooks for learning Latin and Greek came down under the name of Dositheus, a grammarian who did not write
them. Their word lists set the two languages side by side, and some were arranged by subject rather than by
letter: the gods in one place, the heavens in another. A word was found under its heading, and the heading was
the same in both languages.*

A translation file is a JSON object named after its language. Its nesting is the shape of the generated object,
and it has to be the same shape in every language: files that disagree stop the build.

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
module and `Legion.Units` from another. Each key has to be in exactly one of them: the same key in two files of
one language stops the build, and so does a key that is a translation in one file and a group of keys in
another.

A file that does not parse as JSON, or whose top level is not an object, stops the build too.

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

A key is one function for all languages, so **every language must have every key, in the same shape**: a string
in all of them or a plural in all of them, using the same placeholders. The order of the placeholders is free,
and so, in a plural, is the count; the forms of a plural are each language's own.

Anything else stops the build, with every problem listed at once:

```
> Problems in the translation files:
  - en has no `Legion.Orders.retreat`
  - `Legion.greeting` is not the same in every language: a string using {0}, {1} in en; a string using {0} in pl
```

The check covers everything a generator reads. A generator in the root project reads every module, so every
module has to carry every language that any of them has. Until a translation is ready, its key still has to hold
something in that language: the text of another one, for instance.
