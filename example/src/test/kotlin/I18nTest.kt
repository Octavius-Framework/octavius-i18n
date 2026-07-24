package org.example.i18n

import io.github.octaviusframework.i18n.core.OctaviusI18nimport org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class I18nTest {

    @Test
    fun `test english translations`() {
        OctaviusI18n.currentLanguage = "en"
        
        assertEquals("Hello", Tr.hello())
        assertEquals("Welcome Alice", Tr.welcome("Alice"))
        
        assertEquals("Active users", Tr.Users.active())
        assertEquals("1 user", Tr.Users.count(1))
        assertEquals("5 users", Tr.Users.count(5))
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
    }
}
