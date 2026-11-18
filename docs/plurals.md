# Plurals

*Latin counted in two forms: one legionary, many legionaries. Greek had kept a third, the dual, for things that
come in twos — though by the time Rome ruled the Greek east it had all but gone out of speech. A scribe putting a
number into words had to know whose rules he was counting by; the number alone did not tell him.*

A plural key holds one string per form, and the language's rule decides which form a count takes.

Contents:
* [A plural key](#a-plural-key)
* [How a form is chosen](#how-a-form-is-chosen)
* [The rules come from CLDR](#the-rules-come-from-cldr)
* [Which rule a language takes](#which-rule-a-language-takes)
* [Arguments beside the count](#arguments-beside-the-count)
* [Two counts in one sentence](#two-counts-in-one-sentence)

## A plural key

An object with a `$plural` key is a plural key. `$plural` names the count, and the other keys hold the forms:

```json
"cohort": {
  "$plural": "count",
  "one":   "{count} kohorta",
  "few":   "{count} kohorty",
  "many":  "{count} kohort",
  "other": "{count} kohorty"
}
```

It becomes one function, `cohort(count: Number)`, and the count fills `{count}`:

```kotlin
OctaviusI18n.currentLanguage = "pl"
Tr.Legion.Units.cohort(1)      // "1 kohorta"
Tr.Legion.Units.cohort(3)      // "3 kohorty"
Tr.Legion.Units.cohort(12)     // "12 kohort"
Tr.Legion.Units.cohort(22)     // "22 kohorty"
Tr.Legion.Units.cohort(1.5)    // "1.5 kohorty"
```

The count is inserted as Kotlin prints it: `1.5`, with a point, in every language.

**The form is chosen from the count as it prints**, so it agrees with what is read. `Int`, `Long`, `Short` and
`Byte` print no fraction digits; `cohort(1.0)` prints `1.0`, a fraction digit, and reads `1.0 kohorty`. A
`BigDecimal` keeps the digits it was written with. Kotlin/JS prints `1.0` as `1`, and there it is counted as `1`.

| Key                                 | Takes                                                  |
|:------------------------------------|:-------------------------------------------------------|
| `=0`, `=1`, `=2.5`, …               | a count equal to that value, in every language         |
| `zero`, `one`, `two`, `few`, `many` | the form of that name                                  |
| `other`                             | the form `other`, and every form the key does not have |

Each language lists the forms it needs, and the build does not compare them. A language whose words do not change
with the number can hold the key as a plain string, such as `"{count} コホート"`. The build checks only the
arguments besides the count — see [One key, every language](translation-files.md#one-key-every-language).

A plural can also sit in a branch of another choice, or hold one — see [Choices](choices.md).

## How a form is chosen

1. **An exact value is taken first**, in every language and whatever its rule says. `"=0": "no cohorts"` in
   `en.json` makes `cohort(0)` read `no cohorts`; without it, `0 cohorts`. A count matches whatever its type:
   `=0` takes `0` and `0.0` alike.
2. **Otherwise the language's rule names a form.**
3. **A form the key does not have falls back to `other`.**

A key needs only the forms its language tells apart, plus `other`. A plural without `other` stops the build.

**`=0` is not the form `zero`.** Some languages' rules give `zero` to counts other than 0, so a text meant for
nothing at all, such as `no cohorts`, belongs under `=0`.

## The rules come from CLDR

Every language is counted by its rule from CLDR, Unicode's collection of locale data, as of CLDR 49. The plugin
carries the rules and compiles the one of each language it finds a file for into that language's generated code,
so nothing is registered or looked up when the program runs. English and Polish:

| Language | Whole numbers                                                             | With fraction digits |
|:---------|:--------------------------------------------------------------------------|:---------------------|
| `en`     | 1 → `one`; anything else → `other`                                        | `other`              |
| `pl`     | 1 → `one`; ending in 2–4 but not in 12–14 → `few`; anything else → `many` | `other`              |

Polish therefore never takes `other` for a whole number. It is there for fractions, and for any form a key leaves
out. Ukrainian, with three forms of its own, needs nothing more than its file:

```json
"cohort": {
  "$plural": "count",
  "one":   "{count} когорта",
  "few":   "{count} когорти",
  "many":  "{count} когорт",
  "other": "{count} когорти"
}
```

```kotlin
OctaviusI18n.currentLanguage = "uk"
Tr.Legion.Units.cohort(21)    // "21 когорта"
Tr.Legion.Units.cohort(3)     // "3 когорти"
Tr.Legion.Units.cohort(5)     // "5 когорт"
```

Which forms a language has, and which numbers fall into each, is in the
[CLDR plural rules table](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html).

## Which rule a language takes

A file's language code is looked up as it is, then with its last part cut off until CLDR has a rule for it. Case
does not matter, and `_` separates parts as `-` does.

| File         | Rule    |
|:-------------|:--------|
| `pl.json`    | `pl`    |
| `en-GB.json` | `en`    |
| `pt-BR.json` | `pt`    |
| `pt-PT.json` | `pt-PT` |

CLDR has a rule for a region only where the region counts differently. Its `pt` is Brazilian Portuguese, which
counts 0, 1 and every fraction below 2 as `one`; `pt-PT` counts only 1 as `one`. A file named `pt.json` therefore
takes the Brazilian rule.

A language CLDR has no rule for, such as a file named `polish.json`, stops the build once one of its plurals has
a form besides `other`. A plural of exact values and `other` alone needs no rule.

## Arguments beside the count

Any other placeholder in the forms becomes a parameter too:

```json
"built": {
  "$plural": "count",
  "one":   "{count} legionista zbudował {what}",
  "few":   "{count} legionistów zbudowało {what}",
  "many":  "{count} legionistów zbudowało {what}",
  "other": "{count} legionisty zbudowało {what}"
}
```

gives `built(count: Number, what: Any)`. The count comes first because it chooses — see
[Placeholders](translation-files.md#placeholders).

## Two counts in one sentence

A sentence with two counts can be two keys. The second count is rendered by its own key and passed in as an
argument:

```json
"towers": {
  "$plural": "count",
  "one":   "{count} wieżę",
  "few":   "{count} wieże",
  "many":  "{count} wież",
  "other": "{count} wieży"
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

Both counts can also go into one key, with a `$plural` in each branch of another — see
[Choices](choices.md#a-choice-inside-a-choice).
