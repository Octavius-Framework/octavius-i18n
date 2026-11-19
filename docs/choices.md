# Choices

*The praetor did not write a new text for every lawsuit. Gaius sets the formulas out with stock names, Aulus
Agerius who sues and Numerius Negidius who is sued, and with the verdict written both ways: if it appears that he
owes, condemn him; if it does not appear, absolve him. The judge put in the names and took the branch the facts
chose. Wording the verdict was never left to him.*

A key can choose its text by an argument: by a string with `$select`, by a count with `$plural`, by a place in
order with `$ordinal`. Each branch is a whole sentence or another choice, and each language makes only the choices
its grammar needs.

Contents:
* [A choice by a string](#a-choice-by-a-string)
* [A choice inside a choice](#a-choice-inside-a-choice)
* [One key, different choices](#one-key-different-choices)
* [What a choice needs](#what-a-choice-needs)

## A choice by a string

An object with a `$select` key chooses one of its other keys by the argument `$select` names:

```json
"arrived": {
  "$select": "gender",
  "female": "{name} przybyła do Rzymu",
  "other":  "{name} przybył do Rzymu"
}
```

The argument becomes a `String` parameter, `arrived(gender: String, name: Any)`. A value takes the branch whose
name it equals exactly, and a value no branch has takes `other`:

```kotlin
OctaviusI18n.currentLanguage = "pl"
Tr.Legion.arrived("female", "Julia")    // "Julia przybyła do Rzymu"
Tr.Legion.arrived("male", "Marcus")     // "Marcus przybył do Rzymu"
Tr.Legion.arrived("Female", "Livia")    // "Livia przybył do Rzymu"
```

A choice by a count is the same object with `$plural`, whose branches are plural forms — see
[Plurals](plurals.md#a-plural-key). One by a place in order is a `$ordinal` — see
[Plurals](plurals.md#ordinals).

## A choice inside a choice

A branch is a string or another choice. Polish agrees the verb with both the gender and the number:

```json
"citizens": {
  "$select": "gender",
  "female": {
    "$plural": "count",
    "one":   "{count} obywatelka przybyła do Rzymu",
    "few":   "{count} obywatelki przybyły do Rzymu",
    "many":  "{count} obywatelek przybyło do Rzymu",
    "other": "{count} obywatelki przybyło do Rzymu"
  },
  "other": {
    "$plural": "count",
    "one":   "{count} obywatel przybył do Rzymu",
    "few":   "{count} obywateli przybyło do Rzymu",
    "many":  "{count} obywateli przybyło do Rzymu",
    "other": "{count} obywatela przybyło do Rzymu"
  }
}
```

```kotlin
Tr.Legion.citizens(1, "female")   // "1 obywatelka przybyła do Rzymu"
Tr.Legion.citizens(3, "female")   // "3 obywatelki przybyły do Rzymu"
Tr.Legion.citizens(5, "male")     // "5 obywateli przybyło do Rzymu"
```

The arguments that choose come first among the parameters, in alphabetical order, so this is
`citizens(count: Number, gender: String)`. The others follow as [Placeholders](translation-files.md#placeholders)
describes.

## One key, different choices

A key's function takes every argument that any language uses. English does not inflect *arrived*, so its
`arrived` is a plain string:

```json
"arrived": "{name} arrived in Rome"
```

```kotlin
OctaviusI18n.currentLanguage = "en"
Tr.Legion.arrived("female", "Julia")    // "Julia arrived in Rome"
```

The function still takes `gender` because Polish chooses by it, and English ignores it. Leaving out an argument
is allowed only for one that chooses: an argument that is only inserted into the text has to be in every
language — see [One key, every language](translation-files.md#one-key-every-language).

## What a choice needs

- **One `$plural`, `$ordinal` or `$select`**, naming the argument it chooses by, such as `"gender"`.
- **An `other` branch.**
- **Branches that are strings or choices.** A `$select` branch can have any name; those of `$plural` and
  `$ordinal` are listed in [Plurals](plurals.md#a-plural-key).
- **One way to choose by an argument.** An argument that one language counts with `$plural` cannot be a
  `$ordinal` or a `$select` in another.

Each of these stops the build, naming the key, the file and the branches leading to the problem:

```
> Problems in the translation files:
  - `Legion.citizens` in /home/marcus/legion/src/main/i18n/pl.json, under `female`: the `$plural` has no `other`
```
