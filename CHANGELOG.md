## Version 2.0.0 (v2.0.0)

### Plugin

#### Added

- A key can choose its text by an argument. An object with `"$select": "gender"` takes the branch whose name the
  `String` argument equals, or `other`; one with `"$plural": "count"` takes the branch for an exact value such as
  `=0`, then for the count's form, then `other`. A branch is a string or another choice, so one key can choose by
  a gender and a count at once. A choice without `other` stops the build. See [Choices](docs/choices.md).

- A language may leave out an argument that only chooses its text, as English needs no `gender` where Polish
  chooses by it, and may hold a key as a plain string where another holds a plural, such as
  `"{count} コホート"`.

#### Changed

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

- **[BREAKING]** A plural key's accessor takes one `count: Number` instead of an `Int` and a `Double` overload.
  `cohort(5)` and `cohort(1.5)` compile as before; `Long`, `Short` and `Byte` count as whole numbers and `Float`
  as a fraction. On Kotlin/JS a `Double` with no fractional part counts as a whole number, matching the `1`
  Kotlin/JS prints for it, where it counted as a fraction.

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

### Runtime

#### Added

- `OctaviusI18n.selectPluralForm(lang, Number)`, which the generated plurals call. A `Long` too large for an `Int`
  reaches the rule as 1 000 000 000 plus its last nine digits, so a rule that looks at the last digits counts it
  right.

#### Removed

- **[BREAKING]** `TranslationData`, `PluralForms`, `OctaviusI18n.lookup`, `lookupPlural` and `formatString`. The
  generated code no longer calls them.

### Project

#### Added

- The runtime has tests of its own, in `commonTest` and for JVM and JS, run on every target the build machine can
  run.

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
