package org.example.i18n

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import io.github.octaviusframework.i18n.core.OctaviusI18n
import org.example.feature.i18n.FeatureTr

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
        
        // Composition examples
        assertEquals(
            "1 man bought 1 apple", 
            Tr.Composition.sentence(Tr.Composition.men(1), Tr.Composition.apples(1))
        )
        assertEquals(
            "3 men bought 5 apples", 
            Tr.Composition.sentence(Tr.Composition.men(3), Tr.Composition.apples(5))
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

        // Composition examples
        assertEquals(
            "1 mężczyzna kupiło 1 jabłko", 
            Tr.Composition.sentence(Tr.Composition.men(1), Tr.Composition.apples(1))
        )
        assertEquals(
            "3 mężczyzn kupiło 2 jabłka", 
            Tr.Composition.sentence(Tr.Composition.men(3), Tr.Composition.apples(2))
        )
        assertEquals(
            "5 mężczyzn kupiło 10 jabłek", 
            Tr.Composition.sentence(Tr.Composition.men(5), Tr.Composition.apples(10))
        )

        // Submodule test via generic Tr
        assertEquals("Zaloguj do funkcji", Tr.Feature.login())

        // Submodule test via dedicated FeatureTr
        assertEquals("Zaloguj do funkcji", FeatureTr.Feature.login())
    }
}
