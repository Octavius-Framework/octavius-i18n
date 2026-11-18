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

A string becomes a function returning `String`. An object with a `$plural`, `$ordinal` or `$select` key is one
translation too, which chooses its text — see [Plurals](plurals.md) and [Choices](choices.md). Every other object is a group
of keys and becomes a Kotlin `object`; an empty one generates nothing.

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

A name in braces is a placeholder, and its function takes an `Any` parameter of that name, in backticks if it is
a Kotlin keyword. Each argument is inserted with `toString()`.

```json
"greeting": "Ave, {name}! Witaj w legionie {legion}."
```

```kotlin
Tr.Legion.greeting(name = "Marcus", legion = "IX")   // "Ave, Marcus! Witaj w legionie IX."
Tr.Legion.greeting("IX", "Marcus")                   // the same
```

- **The parameters do not follow the text.** They come in alphabetical order, so `greeting` is
  `greeting(legion: Any, name: Any)` in every language, whatever order a translation puts them in. Arguments a
  [choice](choices.md) chooses by come before the rest.
- A name starts with a letter, followed by letters, digits and `_`. Anything else in braces is text, such as
  `{ name }` or `{_x}`, except a number: `{0}` stops the build, which asks for a name instead.
- A placeholder cannot be escaped, so a translation cannot show a literal `{name}`.
- An argument is inserted as it is. An argument that itself contains `{legion}` is not expanded.

## One key, every language

A key is one function for all languages, so **every language must have every key**, and the function takes every
argument that any language uses. Beyond that, each language writes the key its own way: a plain string in one, a
plural or a [choice](choices.md) in another, with the placeholders in any order.

**An argument that is only inserted into the text has to be in every language.** One that a language leaves out is
almost always a placeholder forgotten in translation. An argument that chooses, the count of a plural or the
argument of a `$select`, may be missing from a language that does not need the choice, and a language may insert
a count without choosing by it, as in `"{count} コホート"`.

Anything else stops the build, with every problem listed at once:

```
> Problems in the translation files:
  - en has no `Legion.Orders.retreat`
  - `Legion.greeting` uses {legion} in en but not in pl
```

The check covers everything a generator reads. A generator in the root project reads every module, so every
module has to carry every language that any of them has. Until a translation is ready, its key still has to hold
something in that language: the text of another one, for instance.
