## Version 2.1.0 (v2.1.0)

### Plugin

#### Added

- A JSON Schema of a translation file, `schema/translation-file.schema.json`, lets an editor such as IntelliJ IDEA
  or VS Code mark a mistake in a file as it is typed, where the build found it only when it ran: a number, `null`
  or an array as a translation, a `$plural`, `$ordinal` or `$select` without `other`, a form a plural does not
  take, an unknown `$` key, or a branch that is a group. A file names it in `"$schema"` at its top, which the build
  ignores, where it stopped the build as a key no Kotlin name can have. What takes more than one file, such as a
  key a language lacks, is still the build's. See
  [Translation Files](docs/translation-files.md#a-schema-for-the-editor).

### Project

#### Added

- The plugin's tests check the schema against the build: it is a valid draft-07 schema, accepts every translation
  file of `example/`, and accepts and rejects the same files the build does, for everything a file's shape alone
  decides.

## Version 2.0.0 (v2.0.0) (2026-11-19) - Interpres

### Project

#### Added

- The runtime has tests of its own, run on every target the build machine can run, and the plugin's tests check
  every CLDR rule against the samples CLDR gives for it.

- The plugin's build has an `updateCldr` task, which rewrites all of the plugin's CLDR data from one release of
  `cldr-core` and `cldr-numbers-full`.

- The build finds a JDK 21 for its toolchains through the Foojay resolver, which Gradle 10 requires for a JDK it
  has to download.

### Plugin

#### Added

- A key can choose its text by an argument: `"$select": "gender"` takes the branch the `String` argument names,
  `"$plural": "count"` the one for the count, and either falls back to `other`. A branch can hold another choice,
  so one key can choose by a gender and a count at once. See [Choices](docs/choices.md).

- Every language is counted by its CLDR 49 plural rule, compiled into its generated file, where only English and
  Polish had one and every other language was counted by the English one. A language CLDR has no rule for stops
  the build once one of its plurals has a form besides `other`. See
  [Plurals](docs/plurals.md#the-rules-come-from-cldr).

- `"$ordinal": "number"` chooses by a place in order, as English writes `1st`, `2nd`, `3rd` and `4th`, with the
  forms of the language's CLDR 49 ordinal rule. See [Plurals](docs/plurals.md#ordinals).

- `{price, number}` marks an argument as a number: its parameter is a `Number`, written in the language's number
  format. Other arguments are inserted as they print, so a year or an id is never grouped.

- `Tr.numberFormat`, the current language's number format, for writing another number as its counts are written:
  `Tr.numberFormat.format(12500.5)`.

- `Tr.inLanguage(code)` gives the same keys in another language and leaves the current one alone, so a server can
  answer each user in their own: `Tr.inLanguage("pl").Legion.Orders.march()`. See
  [Choosing the Language](docs/choosing-the-language.md#another-language).

#### Changed

- **[BREAKING]** A group of keys is a property of the generated `{ObjectName}Accessors` class, holding an instance
  of `{Group}_Group`, where it was a nested `object`. Kotlin calls compile as before, and Java goes through
  getters: `Tr.INSTANCE.getLegion().getOrders().march()`.

- **[BREAKING]** The current language finds its file with case and `_` or `-` set aside, and with its last part
  cut off when the whole code has no file: `PL`, `pl_PL` and `pl-PL` all find `pl.json`, where they took the
  fallback language. Two files whose codes differ only that way, such as `pl.json` and `PL.json`, stop the build.

- **[BREAKING]** A plural is an object with `"$plural": "count"` and forms named `one`, `other` and the like, and
  an object of `_one`/`_other` forms is an ordinary group of keys. `_zero` meant both the form `zero` and a count
  of exactly 0, which are now `zero` and `=0`. See [Plurals](docs/plurals.md#a-plural-key).

- **[BREAKING]** A placeholder is a name in braces, `{name}`, and becomes a parameter of that name; parameters
  come in alphabetical order. `{0}`, `{1}`, … stop the build, and a translation that showed a literal `{word}` gets
  a parameter for it. See [Translation Files](docs/translation-files.md#placeholders).

- **[BREAKING]** A number, `true`, `false`, `null` or an array as a translation stops the build, where it became
  its JSON text, so `"triumph": null` showed `null`. A number shown as text is written as a string, `"753"`.

- **[BREAKING]** Each language's translations are compiled into a Kotlin object with one function per key, where
  they were maps looked up on every call. The generated `{ObjectName}Translations{Lang}` objects are `internal`.
  A program loads only the languages and groups it calls.

- **[BREAKING]** A plural's count is written in its language's CLDR 49 number format: Polish writes `1,5` and
  `10 000`, English `1.5` and `10,000`, where both wrote `1.5` and `10000`. See
  [Plurals](docs/plurals.md#how-a-count-is-written).

- **[BREAKING]** A plural key's accessor takes one `count: Number`, where it had an `Int` and a `Double` overload.
  A count is counted by the digits it prints, so on Kotlin/JS, which prints `1.0` as `1`, `1.0` counts as the
  whole number 1, where it counted as a fraction.

- Languages may differ in which arguments choose a key's text. One may choose by an argument another leaves out,
  as Polish chooses by `gender` where English does not, and a key may be a plain string in one language and a
  plural in another, such as `"{count} コホート"`, which stopped the build. An argument only inserted into the text
  still has to be in every language. See [Translation Files](docs/translation-files.md#one-key-every-language).

- An empty object is an empty group and generates nothing, where it was a plural with no forms.

- A file that does not parse is reported at its line and column, which the IDE links to, where the message gave a
  character offset.

#### Fixed

- The text standing in for a language with no file is counted by its own language's plural rule, where it was
  counted by the current language's.

- A name the accessors cannot have, such as `404` or `night watch`, or two names in one group that become one
  Kotlin name, stop the build naming the key, where the generated code failed to compile. See
  [Translation Files](docs/translation-files.md#from-json-to-kotlin).

- A file not named after a language code, such as `en.v2.json`, stops the build, where its generated object did
  not compile. `pl.json.json` is no longer read as `pl`.

- A generator that finds no translation file stops the build and says where it looks. It only logged a warning,
  and the calls to `Tr` did not compile.

- An empty translation file stops the build, where it was skipped and its language left out without a word.

### Runtime

#### Added

- `PluralOperands`, the operands CLDR's plural rules read from a count, and `NumberFormat`, a language's way of
  writing a number. The generated code calls both.

#### Removed

- **[BREAKING]** `TranslationData`, `PluralForms`, `OctaviusI18n.lookup`, `lookupPlural` and `formatString`, which
  the generated code no longer calls.

- **[BREAKING]** `OctaviusI18n.pluralRules`, `PluralRule` and `OctaviusI18n.selectPluralForm`. A language's rule
  is CLDR's, compiled into the generated code, and cannot be registered or replaced.

#### Fixed

- `OctaviusI18n.currentLanguage` is volatile on the JVM and Kotlin/Native, so a switch on one thread is seen by
  every other from its next call, where a thread that only read it had no guarantee of ever seeing it.

## Version 1.2.0 (v1.2.0) (2026-10-30)

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

## Version 1.1.1 (v1.1.1) (2026-10-07)

### Runtime

#### Fixed

- An argument is inserted as it is, even one that contains a placeholder: `"{0} bought {1}"` with `"{1}"` and
  `"apples"` gives `{1} bought apples`, where it gave `apples bought apples`.

## Version 1.1.0 (v1.1.0) (2026-09-23)

### Plugin

#### Changed

- **[BREAKING]** A generator without `sourceProject` reads the project the plugin is applied to and its
  subprojects, where it read the whole build. Applied in the root project it reads the same as before; applied in
  a subproject it reads only that subproject, unless `sourceProject = rootProject` is set.

- Each generator writes to `build/generated/i18n/<generator name>` and clears it before every run, so a renamed
  object or a dropped language no longer stays compiled until a `clean`. A path set by hand to the shared
  `build/generated/kotlin/commonMain` used before needs the new one.

## Version 1.0.1 (v1.0.1) (2026-08-16)

KDoc for the plugin and the runtime, published as a javadoc jar beside each artifact. The code is unchanged.

## Version 1.0.0 (v1.0.0) (2026-07-25) - Livius Andronicus

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
