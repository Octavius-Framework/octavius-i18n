# Plurals

*Latin counted in two forms: one legionary, many legionaries. Greek had kept a third, the dual, for things that
come in twos — though by the time Rome ruled the Greek east it had all but gone out of speech. A scribe putting a
number into words had to know whose rules he was counting by; the number alone did not tell him.*

A plural key holds one string per form, and the language's rule decides which form a count takes.

Contents:
* [A plural key](#a-plural-key)
* [How a form is chosen](#how-a-form-is-chosen)
* [The built-in rules](#the-built-in-rules)
* [A rule of your own](#a-rule-of-your-own)
* [Arguments beside the count](#arguments-beside-the-count)
* [Two counts in one sentence](#two-counts-in-one-sentence)

## A plural key

An object whose keys are all plural forms, each holding a string, is a plural key:

```json
"cohort": {
  "_one":   "{0} kohorta",
  "_few":   "{0} kohorty",
  "_many":  "{0} kohort",
  "_other": "{0} kohorty"
}
```

It becomes two functions, `cohort(count: Int)` and `cohort(count: Double)`, and the count fills `{0}`:

```kotlin
OctaviusI18n.currentLanguage = "pl"
Tr.Legion.Units.cohort(1)      // "1 kohorta"
Tr.Legion.Units.cohort(3)      // "3 kohorty"
Tr.Legion.Units.cohort(12)     // "12 kohort"
Tr.Legion.Units.cohort(22)     // "22 kohorty"
Tr.Legion.Units.cohort(1.5)    // "1.5 kohorty"
```

The count is inserted as Kotlin prints it: `1.5`, with a point, in every language.

| Key      | Form                                               |
|:---------|:---------------------------------------------------|
| `_zero`  | `zero`, and a count of exactly 0 in every language |
| `_one`   | `one`                                              |
| `_two`   | `two`                                              |
| `_few`   | `few`                                              |
| `_many`  | `many`                                             |
| `_other` | `other`, and every form the key does not have      |

An object that mixes these with any other key is an ordinary object.

Each language lists the forms it needs, and the build does not compare them. It checks only that every language
has the key as a plural, using the same placeholders besides the count.

## How a form is chosen

1. **A count of exactly 0 takes `_zero` when the key has one**, in every language and whatever its rule says.
   `"_zero": "no cohorts"` in `en.json` makes `cohort(0)` read `no cohorts`; without it, `0 cohorts`.
2. **Otherwise the language's rule names a form.**
3. **A form the key does not have falls back to `_other`.**
4. **A key without `_other` falls back to its path**, such as `Legion.Units.cohort`.

A key needs only the forms its language tells apart, plus `_other`.

## The built-in rules

Two languages have a rule built in:

| Language | Whole numbers (`Int`)                                                     | Fractions (`Double`) |
|:---------|:--------------------------------------------------------------------------|:---------------------|
| `en`     | 1 → `one`; anything else → `other`                                        | `other`              |
| `pl`     | 1 → `one`; ending in 2–4 but not in 12–14 → `few`; anything else → `many` | `other`              |

Polish therefore never takes `_other` for an `Int`. It is there for fractions, and for any form a key leaves out.

**Every other language is counted by the English rule until it has one of its own.** For a language with one
form that is enough: give the key `_other` alone, and every count takes it. For a language with more forms than
English, it is not.

## A rule of your own

`OctaviusI18n.pluralRules` maps a language code to its `PluralRule`. Ukrainian, which the English rule counts
wrong:

```json
"cohort": {
  "_one":   "{0} когорта",
  "_few":   "{0} когорти",
  "_many":  "{0} когорт",
  "_other": "{0} когорти"
}
```

```kotlin
import io.github.octaviusframework.i18n.core.OctaviusI18n
import io.github.octaviusframework.i18n.core.PluralRule

OctaviusI18n.pluralRules["uk"] = object : PluralRule {
    override fun selectForm(count: Int) = when {
        count % 10 == 1 && count % 100 != 11 -> "one"
        count % 10 in 2..4 && count % 100 !in 12..14 -> "few"
        else -> "many"
    }
    override fun selectForm(count: Double) = "other"
}

OctaviusI18n.currentLanguage = "uk"
Tr.Legion.Units.cohort(21)    // "21 когорта"
Tr.Legion.Units.cohort(3)     // "3 когорти"
Tr.Legion.Units.cohort(5)     // "5 когорт" — "5 когорти" without the rule
```

A rule returns the form's name without the underscore. A name that is not one of the six takes `_other`.

The map is not synchronized: register rules at startup, before anything is translated. Assigning `"pl"` or `"en"`
replaces the built-in rule, and replacing `en` changes it for every language that has no rule of its own.

Which forms a language has, and which numbers fall into each, is in the
[CLDR plural rules table](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html).

## Arguments beside the count

Placeholders above `{0}` in any of the forms become parameters after the count:

```json
"built": {
  "_one":   "{0} legionista zbudował {1}",
  "_few":   "{0} legionistów zbudowało {1}",
  "_many":  "{0} legionistów zbudowało {1}",
  "_other": "{0} legionisty zbudowało {1}"
}
```

gives `built(count: Int, arg1: Any)`, and the same with a `Double` count.

## Two counts in one sentence

A sentence with two counts is two keys. The second count is rendered by its own key and passed in as an argument:

```json
"towers": {
  "_one":   "{0} wieżę",
  "_few":   "{0} wieże",
  "_many":  "{0} wież",
  "_other": "{0} wieży"
}
```

```kotlin
Tr.Legion.Camp.built(1, Tr.Legion.Camp.towers(3))   // "1 legionista zbudował 3 wieże"
Tr.Legion.Camp.built(5, Tr.Legion.Camp.towers(1))   // "5 legionistów zbudowało 1 wieżę"
```

The sentence belongs to the key whose count the verb agrees with. *Zbudował* and *zbudowało* are one verb
agreeing with one legionary and with five, so the legionaries hold the sentence and the towers are passed in.
Turned the other way round, the verb would follow the towers, and one legionary would get the verb of five:
`1 legionista zbudowało 5 wież`.
