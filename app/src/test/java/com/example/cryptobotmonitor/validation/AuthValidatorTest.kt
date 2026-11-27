
package com.example.cryptobotmonitor.validation

import org.junit.Assert.*
import org.junit.Test

class AuthValidatorTest {

    // Testy normalizacji adresu email

    @Test
    fun normalizeEmail_correctEmail() {
        assertEquals(
            "user@gmail.com",
            AuthValidator.normalizeEmail("user@gmail.com")
        )
    }

    @Test
    fun normalizeEmail_withSpaces() {
        assertEquals(
            "user@gmail.com",
            AuthValidator.normalizeEmail("  user@gmail.com  ")
        )
    }

    @Test
    fun normalizeEmail_emptyEmail() {
        assertEquals(
            "",
            AuthValidator.normalizeEmail("   ")
        )
    }

    // Testy poprawnych adresów email

    @Test
    fun isValidEmail_correctEmail() {
        assertTrue(
            AuthValidator.isValidEmail("user@gmail.com")
        )
    }

    @Test
    fun isValidEmail_withNumbers() {
        assertTrue(
            AuthValidator.isValidEmail("user123@gmail.com")
        )
    }

    @Test
    fun isValidEmail_withDot() {
        assertTrue(
            AuthValidator.isValidEmail("user.name@gmail.com")
        )
    }

    @Test
    fun isValidEmail_withPlus() {
        assertTrue(
            AuthValidator.isValidEmail("user+test@gmail.com")
        )
    }

    @Test
    fun isValidEmail_withSubdomain() {
        assertTrue(
            AuthValidator.isValidEmail("user@mail.example.com")
        )
    }

    @Test
    fun isValidEmail_uppercase() {
        assertTrue(
            AuthValidator.isValidEmail("USER@GMAIL.COM")
        )
    }

    @Test
    fun isValidEmail_withSpacesAround() {
        assertTrue(
            AuthValidator.isValidEmail(" user@gmail.com ")
        )
    }

    // Testy niepoprawnych adresów email

    @Test
    fun isValidEmail_emptyEmail() {
        assertFalse(
            AuthValidator.isValidEmail("")
        )
    }

    @Test
    fun isValidEmail_withoutAt() {
        assertFalse(
            AuthValidator.isValidEmail("usergmail.com")
        )
    }

    @Test
    fun isValidEmail_withoutUsername() {
        assertFalse(
            AuthValidator.isValidEmail("@gmail.com")
        )
    }

    @Test
    fun isValidEmail_withoutDomain() {
        assertFalse(
            AuthValidator.isValidEmail("user@")
        )
    }

    @Test
    fun isValidEmail_withoutDomainExtension() {
        assertFalse(
            AuthValidator.isValidEmail("user@gmail")
        )
    }

    @Test
    fun isValidEmail_withInternalSpace() {
        assertFalse(
            AuthValidator.isValidEmail("user name@gmail.com")
        )
    }

    @Test
    fun isValidEmail_multipleAtSigns() {
        assertFalse(
            AuthValidator.isValidEmail("user@@gmail.com")
        )
    }

    @Test
    fun isValidEmail_doubleDot() {
        assertFalse(
            AuthValidator.isValidEmail("user..name@gmail.com")
        )
    }

    @Test
    fun isValidEmail_invalidDomain() {
        assertFalse(
            AuthValidator.isValidEmail("user@-gmail.com")
        )
    }

    // Testy logowania użytkownika

    @Test
    fun login_correctData() {
        assertNull(
            AuthValidator.validateData(
                "user@gmail.com",
                "password123",
                false
            )
        )
    }

    @Test
    fun login_emptyEmail() {
        assertEquals(
            "Wpisz email i hasło.",
            AuthValidator.validateData(
                "",
                "password123",
                false
            )
        )
    }

    @Test
    fun login_emptyPassword() {
        assertEquals(
            "Wpisz email i hasło.",
            AuthValidator.validateData(
                "user@gmail.com",
                "",
                false
            )
        )
    }

    @Test
    fun login_blankPassword() {
        assertEquals(
            "Wpisz email i hasło.",
            AuthValidator.validateData(
                "user@gmail.com",
                "   ",
                false
            )
        )
    }

    @Test
    fun login_invalidEmail() {
        assertEquals(
            "Podaj poprawny adres email.",
            AuthValidator.validateData(
                "invalid-email",
                "password123",
                false
            )
        )
    }

    @Test
    fun login_shortPassword() {
        assertNull(
            AuthValidator.validateData(
                "user@gmail.com",
                "123",
                false
            )
        )
    }

    // Testy rejestracji użytkownika

    @Test
    fun register_correctData() {
        assertNull(
            AuthValidator.validateData(
                "user@gmail.com",
                "password123",
                true
            )
        )
    }

    @Test
    fun register_minimumPasswordLength() {
        assertNull(
            AuthValidator.validateData(
                "user@gmail.com",
                "123456",
                true
            )
        )
    }

    @Test
    fun register_shortPassword() {
        assertEquals(
            "Hasło musi mieć minimum 6 znaków.",
            AuthValidator.validateData(
                "user@gmail.com",
                "12345",
                true
            )
        )
    }

    @Test
    fun register_emptyPassword() {
        assertEquals(
            "Wpisz email i hasło.",
            AuthValidator.validateData(
                "user@gmail.com",
                "",
                true
            )
        )
    }

    @Test
    fun register_invalidEmail() {
        assertEquals(
            "Podaj poprawny adres email.",
            AuthValidator.validateData(
                "usergmail.com",
                "password123",
                true
            )
        )
    }

    @Test
    fun register_longPassword() {
        assertNull(
            AuthValidator.validateData(
                "user@gmail.com",
                "a".repeat(50),
                true
            )
        )
    }

    @Test
    fun register_emailWithSpaces() {
        assertNull(
            AuthValidator.validateData(
                " user@gmail.com ",
                "password123",
                true
            )
        )
    }

    @Test
    fun register_passwordWithSpaces() {
        assertNull(
            AuthValidator.validateData(
                "user@gmail.com",
                " abcde ",
                true
            )
        )
    }
}
