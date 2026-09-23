## Version 1.1.0 (v1.1.0)

### Plugin

#### Changed

- **A generator without `sourceProject` reads the project the plugin is applied to, not the root project.**
  JSON files are still collected from that project and all of its subprojects, so a plugin applied in the root
  project reads the whole build exactly as before. **A plugin applied in a subproject with no `sourceProject`
  used to pick up every `i18n/*.json` in the build and now picks up only its own; set
  `sourceProject = rootProject` to keep the old behaviour.**

- **Every generator writes to its own directory, `build/generated/i18n/<generator name>`, and clears it before
  each run.** Generators used to share `build/generated/kotlin/commonMain`, which was never cleared: renaming
  `objectName` or dropping a language left the old files there, and they kept being compiled until a `clean`.
  Anything that pointed at the old path by hand - an IDE source root, a build script - needs the new one.

## Version 1.0.1 (v1.0.1)

KDoc across the plugin and the runtime, published as a javadoc jar next to each artifact, and README badges
for Maven Central and the Gradle Plugin Portal. Publishing moved to the Vanniktech Maven Publish plugin. The
published artifacts and the code they contain are unchanged.

## Version 1.0.0 (v1.0.0)

The first release: a Gradle plugin that turns `i18n/<lang>.json` files into type-safe Kotlin accessors, and
a Kotlin Multiplatform runtime they call into.

**Plugin** (`io.github.octavius-framework.i18n`)

- `octaviusI18n { generators { ... } }` - any number of generators per project, each with its own
  `targetPackage`, `objectName` (default `Tr`), `fallbackLanguage` (default `en`) and `sourceProject`
- A generation task per generator, wired into Kotlin Multiplatform `commonMain` or Kotlin JVM `main` sources
  automatically - no task to run by hand
- One accessor per key, nested objects per JSON level; `{0}`, `{1}` placeholders become typed parameters
- Plural keys (`_zero`, `_one`, `_two`, `_few`, `_many`, `_other`) become functions taking the count, for
  `Int` and `Double`
- Keys are unioned across languages, so a key missing from one file still gets an accessor and renders as its
  raw path at runtime

**Runtime** (`i18n-core`)

- `OctaviusI18n.currentLanguage` - the active language, global and switchable at runtime
- `OctaviusI18n.pluralRules` - `PluralRule` per language, English and Polish built in, open to your own; an
  unknown language falls back to English, then to `other`
- Targets: JVM, JS, Wasm (JS and WASI), Linux, Windows (mingw), macOS Arm64 and iOS
