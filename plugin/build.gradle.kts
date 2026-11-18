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
    google()
    gradlePluginPortal()
}

dependencies {
    compileOnly(gradleApi())
    implementation(libs.kotlinx.serialization.json)
    compileOnly(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    testImplementation(kotlin("test"))
    // The runtime's operands, to check the CLDR rules against CLDR's own samples
    testImplementation(project(":i18n-core"))
}

tasks.test {
    useJUnitPlatform()
}

// Rewrites cldr/numbers.json, the number format of every CLDR locale, from CLDR's own files:
//   ./gradlew :i18n-plugin:cldrNumberFormats -PcldrNumbers=<dir> -PnumberingSystems=<file>
// with absolute paths: <dir> is the unpacked cldr-numbers-full package from npm, and <file> is
// cldr-json/cldr-core/supplemental/numberingSystems.json of unicode-org/cldr-json, both of one CLDR release.
tasks.register("cldrNumberFormats") {
    description = "Extracts every CLDR locale's number format into src/main/resources/.../cldr/numbers.json."
    val numbers = providers.gradleProperty("cldrNumbers")
    val systems = providers.gradleProperty("numberingSystems")
    val output = layout.projectDirectory.file("src/main/resources/io/github/octaviusframework/i18n/cldr/numbers.json").asFile
    doLast {
        @Suppress("UNCHECKED_CAST")
        fun obj(value: Any?) = value as Map<String, Any?>
        val slurper = groovy.json.JsonSlurper()
        val numbersDir = File(numbers.get())
        val numberingSystems = obj(obj(obj(slurper.parse(File(systems.get())))["supplemental"])["numberingSystems"])

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

        val version = obj(slurper.parse(numbersDir.resolve("package.json")))["version"] as String
        val data = linkedMapOf("cldrVersion" to version.substringBefore('.'), "digits" to digits, "formats" to formats, "locales" to locales)
        output.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(data)) + "\n")
        logger.lifecycle("${formats.size} formats of ${locales.size} locales, CLDR $version")
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
