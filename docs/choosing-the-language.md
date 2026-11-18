# Choosing the Language

*Valerius Maximus records that magistrates of the old Republic answered Greeks only in Latin, and made them speak
through an interpreter, not only in Rome but in Greece and Asia as well. Whoever came before them was heard in the
language of the court rather than his own, and everyone in the room heard the same one.*

The generated objects do not pick a language. They read one that belongs to the whole program.

## The current language

`OctaviusI18n.currentLanguage` is the language every accessor reads, on every call. It starts as `"en"`.

```kotlin
OctaviusI18n.currentLanguage = "pl"
Tr.Legion.Orders.march()   // "Marsz!"
```

**There is one for the whole program.** It belongs to a global object, not to a thread, a request or a generated
object: every `Tr` in every module reads the same one, and switching it switches them all. A text in another
language is asked for by name — see [Another language](#another-language).

It is not synchronized. Switch the language from one thread, the UI thread for example.

## Another language

`Tr.inLanguage(code)` gives the same keys in the language of `code`, and leaves the current language alone:

```kotlin
OctaviusI18n.currentLanguage = "en"
val polish = Tr.inLanguage("pl")
polish.Legion.Orders.march()   // "Marsz!"
Tr.Legion.Orders.march()       // "March!"
```

It finds the language as the current one is found, and keeps it: `polish` translates into Polish whatever the
current language becomes, so a server can answer each user in their own. Every code that finds one language
gives the same object, made the first time that language is asked for, so `Tr.inLanguage(user.language)` can be
called for every text.

## A language with no file

A code finds its file in three steps. The code itself, with case and `_` or `-` set aside, so `PL` and `pl_PL`
find `pl-PL.json` as `pl-PL` does. Then the code with its last part cut off, as many times as it has parts, so
`pl-PL` finds `pl.json`. Then, when none of these has a file, the generator's `fallbackLanguage`, and when that has
no file either, one of the languages that do:

```kotlin
OctaviusI18n.currentLanguage = "de"   // there is no de.json
Tr.Legion.Orders.march()             // "March!", from en.json
```

Each generator has its own `fallbackLanguage`, so two objects can fall back to different languages. The text that
stands in is counted by its own language's plural rule, not by the current language's.

The fallback stands in for a whole language, never for a single key: a language that has a file has every key,
or the build would have stopped. See [Translation Files](translation-files.md#one-key-every-language).

## Plural rules

Each language's plural rule is CLDR's, compiled into its generated file. Which rule a file's language code takes is
in [Plurals](plurals.md#which-rule-a-language-takes).
