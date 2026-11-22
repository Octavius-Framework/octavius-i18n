plugins {
    alias(libs.plugins.kotlinJvm)
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "2.1.1"
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    implementation(libs.kotlinx.serialization.json)
    compileOnly(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    testImplementation(kotlin("test"))
    // The runtime's operands, to check the CLDR rules against CLDR's own samples
    testImplementation(project(":i18n-core"))
    // To check that the translation file's JSON Schema accepts what the plugin reads
    testImplementation(libs.json.schema.validator)
}

tasks.test {
    useJUnitPlatform()
    // The JSON Schema of a translation file, and the example's translation files, which it has to accept
    val schema = rootProject.file("schema/translation-file.schema.json")
    val example = rootProject.file("example")
    inputs.file(schema).withPropertyName("translationSchema").withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(fileTree(example) { include("**/src/**/i18n/*.json") })
        .withPropertyName("exampleTranslations").withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("translationSchema", schema.path)
    systemProperty("example", example.path)
}

// Rewrites the plugin's CLDR data, src/main/resources/io/github/octaviusframework/i18n/cldr/, from one CLDR release:
//   ./gradlew :i18n-plugin:updateCldr -PcldrCore=<dir> -PcldrNumbers=<dir>
// with absolute paths to the unpacked cldr-core and cldr-numbers-full packages from npm, of the same version.
// plurals.json, ordinals.json and LICENSE are copied as they are; numbers.json holds each locale's number format,
// taken from cldr-numbers-full and the digits of cldr-core's numberingSystems.json.
tasks.register("updateCldr") {
    description = "Rewrites the plugin's CLDR plural rules, ordinal rules and number formats from one CLDR release."
    val core = providers.gradleProperty("cldrCore")
    val numbers = providers.gradleProperty("cldrNumbers")
    val output = layout.projectDirectory.dir("src/main/resources/io/github/octaviusframework/i18n/cldr").asFile
    doLast {
        @Suppress("UNCHECKED_CAST")
        fun obj(value: Any?) = value as Map<String, Any?>
        val slurper = groovy.json.JsonSlurper()
        val coreDir = File(core.get())
        val numbersDir = File(numbers.get())

        fun cldrVersion(file: File) =
            obj(obj(obj(slurper.parse(file))["supplemental"])["version"])["_cldrVersion"] as String
        val coreVersion = cldrVersion(coreDir.resolve("supplemental/plurals.json"))
        val numbersVersion = (obj(slurper.parse(numbersDir.resolve("package.json")))["version"] as String).substringBefore('.')
        check(coreVersion == numbersVersion) { "cldr-core is CLDR $coreVersion and cldr-numbers-full CLDR $numbersVersion" }

        for (name in listOf("plurals.json", "ordinals.json")) {
            coreDir.resolve("supplemental/$name").copyTo(output.resolve(name), overwrite = true)
        }
        coreDir.resolve("LICENSE").takeIf { it.isFile }?.copyTo(output.resolve("LICENSE"), overwrite = true)

        val numberingSystems = obj(obj(obj(slurper.parse(coreDir.resolve("supplemental/numberingSystems.json")))["supplemental"])["numberingSystems"])
        val formats = mutableListOf<Map<String, Any>>()
        val locales = sortedMapOf<String, Int>()
        val digits = sortedMapOf<String, List<String>>()
        for (dir in numbersDir.resolve("main").listFiles()!!.sortedBy { it.name.lowercase() }) {
            val n = obj(obj(obj(obj(slurper.parse(dir.resolve("numbers.json")))["main"])[dir.name])["numbers"])
            val system = n["defaultNumberingSystem"] as String
            val symbols = obj(n["symbols-numberSystem-$system"])
            // The grouping of the standard pattern's integer part: #,##0 groups by 3, #,##,##0 by 3 then by 2
            val groups = (obj(n["decimalFormats-numberSystem-$system"])["standard"] as String).substringBefore('.').split(',')
            val primary = if (groups.size > 1) groups.last().length else 0
            val secondary = if (groups.size > 2) groups[groups.size - 2].length else primary
            val format = linkedMapOf<String, Any>(
                "decimal" to symbols["decimal"]!!,
                "group" to symbols["group"]!!,
                "minus" to symbols["minusSign"]!!,
                "minimumGrouping" to (n["minimumGroupingDigits"] as String).toInt(),
                "primaryGrouping" to primary,
                "secondaryGrouping" to secondary,
                "digits" to system,
            )
            digits.getOrPut(system) {
                val text = obj(numberingSystems[system])["_digits"] as String
                generateSequence(0) { it + Character.charCount(text.codePointAt(it)) }.takeWhile { it < text.length }
                    .map { String(Character.toChars(text.codePointAt(it))) }.toList()
            }
            val index = formats.indexOf(format).takeIf { it >= 0 } ?: formats.size.also { formats += format }
            locales[dir.name.lowercase()] = index
        }

        val data = linkedMapOf("cldrVersion" to numbersVersion, "digits" to digits, "formats" to formats, "locales" to locales)
        output.resolve("numbers.json").writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(data)) + "\n")
        logger.lifecycle("CLDR $coreVersion: plural and ordinal rules, and ${formats.size} number formats of ${locales.size} locales")
    }
}

gradlePlugin {
    website.set("https://github.com/octavius-framework/octavius-i18n")
    vcsUrl.set("https://github.com/octavius-framework/octavius-i18n.git")
    
    plugins {
        create("i18n") {
            id = "io.github.octavius-framework.i18n"
            displayName = "Octavius I18n"
            description = "A type-safe, code-generated localization plugin. Write your translations once in JSON, run the generator, and let the compiler catch every missing key — just as a Roman scribe would catch every missing seal."
            tags.set(listOf("kotlin", "i18n", "localization", "kmp", "multiplatform"))
            implementationClass = "io.github.octaviusframework.i18n.I18nPlugin"
        }
    }
}
