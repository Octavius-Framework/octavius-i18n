## Version 2.0.0 (v2.0.0)

### Plugin

#### Added

- A key can choose its text by an argument. An object with `"$select": "gender"` takes the branch whose name the
  `String` argument equals, or `other`; one with `"$plural": "count"` takes the branch for an exact value such as
  `=0`, then for the count's form, then `other`. A branch is a string or another choice, so one key can choose by
  a gender and a count at once. A choice without `other` stops the build. See [Choices](docs/choices.md).

- Every language is counted by its plural rule from CLDR 49, which the plugin carries and compiles into the
  language's generated file. Only English and Polish had a rule built in, and every other language was counted by
  the English one until a rule was registered for it. A file's language code takes the rule of its language when
  CLDR has none for the code itself, as `pt-BR` takes `pt`. A language CLDR has no rule for stops the build once
  one of its plurals has a form besides `other`. See [Plurals](docs/plurals.md#the-rules-come-from-cldr).

- `"$ordinal": "number"` chooses by a place in order, as English writes `1st`, `2nd`, `3rd` and `4th`. It is
  written as a `$plural` is, and its forms come from the language's CLDR 49 ordinal rule. A language whose rule
  names only `other`, as Polish's does, or that CLDR has no ordinal rule for, counts every place as `other`. One
  argument cannot be a `$plural` in one language and a `$ordinal` in another. See
  [Plurals](docs/plurals.md#ordinals).

- A language may leave out an argument that only chooses its text, as English needs no `gender` where Polish
  chooses by it, and may hold a key as a plain string where another holds a plural, such as
  `"{count} コホート"`.

- `Tr.numberFormat`, the number format of the current language, the one its counts are written in, for writing
  another number the same way: `Tr.Legion.pay(Tr.numberFormat.format(12500.5))`. It is a property, so no key can
  take its name.

- `{price, number}` marks an argument as a number: its parameter is a `Number`, and it is written in the
  language's number format, as a count is, in every language. Other arguments are inserted as they print, so a
  year or an id is never grouped. A name with anything else after a comma, such as `{price, numbr}`, stops the
  build.

- `Tr.inLanguage(code)`, the same keys in another language than the current one, which it leaves alone:
  `Tr.inLanguage("pl").Legion.Orders.march()`. The language is found as the current one is and kept, so a server
  can answer each user in their own. Every code that finds one language gives the same object, made the first
  time that language is asked for, so it can be called for every text. See
  [Choosing the Language](docs/choosing-the-language.md#another-language).

#### Changed

- **[BREAKING]** A group of keys is a property of the generated `{ObjectName}Accessors` class, holding an instance
  of its inner class `{Group}Group`, where it was a nested `object`; `Tr` is an instance of that class too. Kotlin
  calls such as `Tr.Legion.Orders.march()` compile as before, and Java goes through getters:
  `Tr.INSTANCE.getLegion().getOrders().march()`.

- **[BREAKING]** The current language finds its file with case and `_` or `-` set aside, and with its last part
  cut off when the whole code has no file: `PL`, `pl_PL` and `pl-PL` all find `pl.json`, where each needed a file
  of exactly that name and took the fallback language without one. Two files whose codes differ only that way,
  such as `pl.json` and `PL.json`, stop the build.

- **[BREAKING]** A plural is written with `$plural`, and an object of `_one`/`_other` forms is an ordinary group
  of keys. `{"_one": "{0} kohorta", "_other": "{0} kohorty"}` becomes
  `{"$plural": "count", "one": "{count} kohorta", "other": "{count} kohorty"}`. `_zero` meant both the form `zero`
  and a count of exactly 0; the two are `zero` and `=0`, since some languages' rules give `zero` to counts other
  than 0.

- **[BREAKING]** A placeholder is a name in braces, `{name}`, and its parameter has that name. `{0}`, `{1}`, …
  stop the build, so `"Ave, {0}!"` becomes `"Ave, {name}!"` and `greeting(arg0)` becomes `greeting(name)`.
  Parameters come in alphabetical order, whatever order a translation puts its placeholders in, with the
  arguments that choose first. A translation that showed a literal `{word}` gets a parameter for it.

- **[BREAKING]** Each language's translations are compiled into a Kotlin object with one function per key, where
  they were maps looked up by key on every call. The generated `{ObjectName}Translations{Lang}` objects are
  `internal` and implement a generated `internal` interface instead of `TranslationData`.

- **[BREAKING]** A count, the argument a `$plural` or a `$ordinal` chooses by, is written in its language's number
  format from CLDR 49, which the plugin carries and compiles into the language's file: its decimal separator, its
  grouping, its minus sign and its digits. Polish writes `1,5` and `10 000`, English `1.5` and `10,000`, where
  both wrote `1.5` and `10000`. The digits are the ones Kotlin prints, neither rounded nor padded. Every other
  argument not marked `{name, number}` is inserted as it prints, as before. See
  [Plurals](docs/plurals.md#how-a-count-is-written).

- **[BREAKING]** A plural key's accessor takes one `count: Number` instead of an `Int` and a `Double` overload.
  `cohort(5)` and `cohort(1.5)` compile as before. A count is counted by the digits it prints, which are the
  digits CLDR's rules read: `1.0` has a fraction digit, a `BigDecimal` keeps the digits it was written with, and
  on Kotlin/JS, which prints `1.0` as `1`, it counts as the whole number 1, where it counted as a fraction.

- The build compares what each language does with a key's arguments, not whether it has the same placeholders. It
  stops on an argument inserted into the text in one language but missing from another, and on an argument that
  chooses a plural form in one language and a case in another. A string in one language and a plural in another,
  which used to stop the build, passes when the string leaves out only the count.

- An empty object is an empty group and generates nothing. It used to be a plural with no forms.

- A file that does not parse is reported at the line and column of the problem, as
  `src/main/i18n/pl.json:3:17 is not valid JSON: Expected end of the object or comma`, which the IDE links to the
  place. The message gave a character offset. Its language is not also reported as lacking every key the file
  held.

#### Fixed

- When the current language has no file, the text standing in for it is counted by its own language's plural
  rule. It was counted by the current language's rule, giving the words of one language the forms of another.

- A name the accessors cannot have stops the build, naming the key, where the generated code failed to compile
  with errors pointing into itself: a key or group whose name, with `_` and `-` set aside, does not start with a
  letter or holds anything but letters and digits, such as `404` or `night watch`; two names in one group that
  become one Kotlin name, such as `siege_engines` and `SiegeEngines`; and a key named `toString` or `hashCode`, or
  `inLanguage` at the top level. See [Translation Files](docs/translation-files.md#from-json-to-kotlin).

- A file not named after a language code, such as `en.v2.json`, stops the build, where its language's generated
  object got a name that did not compile. The code is the whole file name without `.json`, so `pl.json.json` is
  no longer taken for `pl`. See [Translation Files](docs/translation-files.md#the-language-of-a-file).

- A generator that finds no translation file stops the build, saying where it looks. It generated nothing and only
  logged a warning, so the code calling `Tr` failed to compile with an unresolved reference.

- An empty translation file stops the build, as one that is not valid JSON does. It was skipped, so an empty
  `de.json` left German out without a word.

### Runtime

#### Added

- `PluralOperands`, the operands of a count that CLDR's plural rules read, taken from the count as it prints. The
  generated rules call it.

- `NumberFormat`, a language's way of writing a number: its separators, its grouping, its minus sign and its
  digits. The generated code builds each language's from CLDR.

#### Removed

- **[BREAKING]** `TranslationData`, `PluralForms`, `OctaviusI18n.lookup`, `lookupPlural` and `formatString`. The
  generated code no longer calls them.

- **[BREAKING]** `OctaviusI18n.pluralRules`, `PluralRule` and `OctaviusI18n.selectPluralForm`. The plural rules
  are CLDR's, compiled into the generated code, and a rule can no longer be registered or replaced at runtime.

### Project

#### Added

- The runtime has tests of its own, in `commonTest` and for JVM and JS, run on every target the build machine can
  run.

- The plugin's tests check every CLDR rule against the samples CLDR gives for it: 12 259 for the plural rules of
  225 languages and 2 732 for the ordinal rules of 109.

- The plugin's build has an `updateCldr` task, which rewrites all of the plugin's CLDR data from one release: it
  copies the plural and ordinal rules and the license from an unpacked `cldr-core` package, and extracts every
  locale's number format from `cldr-numbers-full`. It refuses packages of two releases, and a test checks that the
  plugin's three data files are of one.

- The build finds a JDK 21 for its toolchains through the Foojay resolver, which Gradle 10 requires for a JDK it
  has to download, and it no longer warns about the runtime's test tasks a machine cannot run.

## Version 1.2.0 (v1.2.0)

### Plugin

#### Changed

- **[BREAKING]** Translation files that disagree stop the build, which lists every problem at once: a key one
  language lacks, a key that is a string in one language and a plural in another or uses other placeholders, and
  a key defined twice within one language. A key one language lacked used to render as its path there, so a build
  that passed on 1.1.1 can fail until every language has every key.

- **[BREAKING]** A translation file that does not parse, or is not a JSON object, stops the build. It used to be
  logged and skipped.

### Project

#### Changed

- The guides are in [`docs/`](docs/README.md), laid out as octavius-postgresql's are, and the README points at
  them.

## Version 1.1.1 (v1.1.1)

### Runtime

#### Fixed

- An argument is inserted as it is, even one that contains a placeholder: `"{0} bought {1}"` with `"{1}"` and
  `"apples"` gives `{1} bought apples`, where it gave `apples bought apples`.

## Version 1.1.0 (v1.1.0)

### Plugin

#### Changed

- **[BREAKING]** A generator without `sourceProject` reads the project the plugin is applied to and its
  subprojects, where it read the whole build. Applied in the root project it reads the same as before; applied in
  a subproject it reads only that subproject, unless `sourceProject = rootProject` is set.

- Each generator writes to `build/generated/i18n/<generator name>` and clears it before every run, so a renamed
  object or a dropped language no longer stays compiled until a `clean`. A path set by hand to the shared
  `build/generated/kotlin/commonMain` used before needs the new one.

## Version 1.0.1 (v1.0.1)

KDoc for the plugin and the runtime, published as a javadoc jar beside each artifact. The code is unchanged.

## Version 1.0.0 (v1.0.0)

The first release.

### Plugin

#### Added

- `octaviusI18n { generators { } }` declares any number of generators, each with its own `targetPackage`,
  `objectName` (default `Tr`), `fallbackLanguage` (default `en`) and `sourceProject`.
- A generator's sources go into Kotlin JVM `main` or Kotlin Multiplatform `commonMain`, so there is no task to run
  by hand.
- Every key is a function and every JSON object a nested object; `{0}`, `{1}` become its parameters.
- Plural keys (`_zero`, `_one`, `_two`, `_few`, `_many`, `_other`) become functions taking an `Int` or a `Double`
  count.
- A key missing from one language still gets an accessor, and renders as its path in that language.

### Runtime

#### Added

- `OctaviusI18n.currentLanguage` is the language every accessor reads, global and switchable at runtime.
- `OctaviusI18n.pluralRules` holds a `PluralRule` per language, with English and Polish built in. A language
  without one is counted by the English rule.
- Targets: JVM, JS, Wasm (JS and WASI), Linux, Windows, macOS Arm64 and iOS.
