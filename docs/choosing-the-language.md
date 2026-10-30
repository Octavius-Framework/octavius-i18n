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
object: every `Tr` in every module reads the same one, and switching it switches them all. An application that
has to render two languages at the same time — a server answering users in each — cannot keep them apart with it.

Neither it nor `pluralRules` is synchronized. Switch the language from one thread, the UI thread for example, and
register plural rules at startup.

## A language with no file

When the current language has no file, the generated object uses its generator's `fallbackLanguage`, and when
that has no file either, one of the languages that do:

```kotlin
OctaviusI18n.currentLanguage = "de"   // there is no de.json
Tr.Legion.Orders.march()             // "March!", from en.json
```

Each generator has its own `fallbackLanguage`, so two objects can fall back to different languages.

## A key missing from a language

A language that has a file but not the key is not filled in from the fallback. The accessor returns the key's
path:

```kotlin
OctaviusI18n.currentLanguage = "en"   // en.json has no Orders.retreat
Tr.Legion.Orders.retreat()            // "Legion.Orders.retreat"
```

`fallbackLanguage` stands in for a whole language, never for a single key.

## Plural rules

`OctaviusI18n.pluralRules` holds the rule for each language, with English and Polish built in. Adding one, and
what a language without one is counted by, are in [Plurals](plurals.md#a-rule-of-your-own).
