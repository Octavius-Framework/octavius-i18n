# Octavius I18n Documentation

*Augustus left an account of his own deeds to be cut in bronze before his mausoleum. The bronze is lost; the text
survives because the provinces copied it, and at Ancyra it went up on the temple of Rome and Augustus twice — in
Latin, and in Greek for the people who would read it there. One text, written once, set up in each place in the
language of that place.*

The plugin turns translation files into code when the project is built; the runtime picks the language and the
plural form when that code runs. The pages follow that order.

| Document                                          | Description                                                                                        |
|:--------------------------------------------------|:---------------------------------------------------------------------------------------------------|
| [Quickstart](quickstart.md)                       | The plugin and the runtime, a first pair of files, every generator option, and what the build does |
| [Translation Files](translation-files.md)         | Which files are read, how JSON becomes functions, placeholders, and the files the build refuses    |
| [Plurals](plurals.md)                             | Plural keys, how a form is chosen, the built-in rules and your own, two counts in one sentence     |
| [Choosing the Language](choosing-the-language.md) | The current language, one for the whole program, and the fallback for a language with no file      |

If nothing is generated yet, the [Quickstart](quickstart.md) is where to start.
