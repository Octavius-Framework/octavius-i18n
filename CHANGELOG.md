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
