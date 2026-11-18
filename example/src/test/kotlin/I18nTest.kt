package org.example.i18n

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import io.github.octaviusframework.i18n.core.OctaviusI18n
import org.example.feature.i18n.FeatureTr

/**
 * End-to-end tests exercising the generated `Tr` / `FeatureTr` accessors against
 * both the English and Polish translation data, including plural forms and
 * cross-module composition.
 */
class I18nTest {

    @Test
    fun `test english translations`() {
        OctaviusI18n.currentLanguage = "en"
        
        assertEquals("Hello", Tr.hello())
        assertEquals("Welcome Alice", Tr.welcome("Alice"))
        
        assertEquals("Active users", Tr.Users.active())
        assertEquals("1 user", Tr.Users.count(1))
        assertEquals("5 users", Tr.Users.count(5))
        
        assertEquals("Alice bought 1 item", Tr.Users.purchased(1, "Alice"))
        assertEquals("Alice bought 5 items", Tr.Users.purchased(5, "Alice"))
        
        // Composition: the verb agrees with the men, so they hold the sentence and the apples are passed in
        assertEquals(
            "1 man bought 1 apple",
            Tr.Composition.bought(1, Tr.Composition.apples(1))
        )
        assertEquals(
            "3 men bought 5 apples",
            Tr.Composition.bought(3, Tr.Composition.apples(5))
        )
        
        // Submodule test via generic Tr
        assertEquals("Login to feature", Tr.Feature.login())
        
        // Submodule test via dedicated FeatureTr
        assertEquals("Login to feature", FeatureTr.Feature.login())
    }

    @Test
    fun `test polish translations`() {
        OctaviusI18n.currentLanguage = "pl"
        
        assertEquals("Cześć", Tr.hello())
        assertEquals("Witaj Bob", Tr.welcome("Bob"))
        
        assertEquals("Aktywni użytkownicy", Tr.Users.active())
        assertEquals("1 użytkownik", Tr.Users.count(1))
        assertEquals("2 użytkowników", Tr.Users.count(2))
        assertEquals("5 użytkowników", Tr.Users.count(5))

        assertEquals("Bob kupił 1 przedmiot", Tr.Users.purchased(1, "Bob"))
        assertEquals("Bob kupił 3 przedmioty", Tr.Users.purchased(3, "Bob"))
        assertEquals("Bob kupił 5 przedmiotów", Tr.Users.purchased(5, "Bob"))

        // Composition: "kupił" agrees with one man, "kupiło" with three or five
        assertEquals(
            "1 mężczyzna kupił 1 jabłko",
            Tr.Composition.bought(1, Tr.Composition.apples(1))
        )
        assertEquals(
            "3 mężczyzn kupiło 2 jabłka",
            Tr.Composition.bought(3, Tr.Composition.apples(2))
        )
        assertEquals(
            "5 mężczyzn kupiło 10 jabłek",
            Tr.Composition.bought(5, Tr.Composition.apples(10))
        )

        // Submodule test via generic Tr
        assertEquals("Zaloguj do funkcji", Tr.Feature.login())

        // Submodule test via dedicated FeatureTr
        assertEquals("Zaloguj do funkcji", FeatureTr.Feature.login())
    }

    @Test
    fun `test argument values are not treated as placeholders`() {
        OctaviusI18n.currentLanguage = "en"

        // An argument containing "{what}" or "{name}" is inserted as it is, not replaced by another argument
        assertEquals("{what} bought apples", Tr.Composition.sentence(who = "{what}", what = "apples"))
        assertEquals("{name} joined", Tr.Users.joined("female", "{name}"))
    }

    @Test
    fun `test a select chooses by a string, and a language that does not need it ignores it`() {
        OctaviusI18n.currentLanguage = "pl"
        assertEquals("Anna dołączyła", Tr.Users.joined("female", "Anna"))
        assertEquals("Jan dołączył", Tr.Users.joined("male", "Jan"))

        OctaviusI18n.currentLanguage = "en"
        assertEquals("Anna joined", Tr.Users.joined("female", "Anna"))
        assertEquals("Jan joined", Tr.Users.joined(gender = "male", name = "Jan"))
    }

    @Test
    fun `test a plural nested in a select, with an exact zero`() {
        OctaviusI18n.currentLanguage = "pl"
        assertEquals("Anna nikogo nie zaprosiła", Tr.Users.invited(0, "female", "Anna"))
        assertEquals("Anna zaprosiła 1 znajomego", Tr.Users.invited(1, "female", "Anna"))
        assertEquals("Anna zaprosiła 3 znajomych", Tr.Users.invited(3, "female", "Anna"))
        assertEquals("Jan nikogo nie zaprosił", Tr.Users.invited(0.0, "male", "Jan"))
        assertEquals("Jan zaprosił 5 znajomych", Tr.Users.invited(5, "male", "Jan"))
        assertEquals("Jan zaprosił 1.5 znajomego", Tr.Users.invited(1.5, "male", "Jan"))

        OctaviusI18n.currentLanguage = "en"
        assertEquals("Anna invited nobody", Tr.Users.invited(0, "female", "Anna"))
        assertEquals("Anna invited 1 friend", Tr.Users.invited(1, "female", "Anna"))
        assertEquals("Anna invited 2 friends", Tr.Users.invited(2, "female", "Anna"))
    }

    @Test
    fun `test a count of any number type`() {
        OctaviusI18n.currentLanguage = "pl"
        assertEquals("Bob kupił 3 przedmioty", Tr.Users.purchased(3L, "Bob"))
        assertEquals("Bob kupił 3 przedmioty", Tr.Users.purchased(3.toShort(), "Bob"))
        // Beyond Int, the last digits still decide: 3 000 000 002 ends in 2, as 2 does
        assertEquals("Bob kupił 3000000002 przedmioty", Tr.Users.purchased(3_000_000_002L, "Bob"))
        assertEquals("Bob kupił 1.5 przedmiotów", Tr.Users.purchased(1.5, "Bob"))
        assertEquals("Bob kupił 1.5 przedmiotów", Tr.Users.purchased(1.5f, "Bob"))

        // `=0` is used for exactly 0 in a language that has it, and the rule decides in one that does not
        OctaviusI18n.currentLanguage = "en"
        assertEquals("No users", Tr.Users.count(0))
        assertEquals("No users", Tr.Users.count(0.0))
        OctaviusI18n.currentLanguage = "pl"
        assertEquals("0 użytkowników", Tr.Users.count(0))
    }

    @Test
    fun `test an ordinal takes the form of its place`() {
        OctaviusI18n.currentLanguage = "en"
        val places = listOf(1, 2, 3, 4, 11, 12, 13, 21, 22, 23, 101, 111).map { Tr.Users.finished(place = it, name = "Anna") }
        assertEquals(
            listOf("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "22nd", "23rd", "101st", "111th").map { "Anna finished $it" },
            places,
        )

        // Polish writes an ordinal as the number and a full stop, so its rule names only `other`
        OctaviusI18n.currentLanguage = "pl"
        assertEquals("Jan zajął 2. miejsce", Tr.Users.finished(2, "Jan"))
    }

    @Test
    fun `test a count is counted by the digits it prints`() {
        OctaviusI18n.currentLanguage = "en"
        assertEquals("1 user", Tr.Users.count(1))
        assertEquals("1.0 users", Tr.Users.count(1.0))
        assertEquals("1.50 users", Tr.Users.count(java.math.BigDecimal("1.50")))
        OctaviusI18n.currentLanguage = "pl"
        assertEquals("1 użytkownik", Tr.Users.count(1))
        assertEquals("1.0 użytkowników", Tr.Users.count(1.0))
    }
}
