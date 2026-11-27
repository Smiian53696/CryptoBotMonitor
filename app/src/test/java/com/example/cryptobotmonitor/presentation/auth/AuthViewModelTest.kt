
package com.example.cryptobotmonitor.presentation.auth

import com.example.cryptobotmonitor.fake.FakeAuthRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthViewModelTest {

    private lateinit var repository: FakeAuthRepository

    private lateinit var viewModel: AuthViewModel

    // Przygotowanie danych przed każdym testem
    @Before
    fun setUp() {

        repository = FakeAuthRepository()

        viewModel = AuthViewModel(repository)
    }

    // Ustawienie poprawnych danych
    private fun enterValidData() {

        viewModel.onEmailChange("user@gmail.com")
        viewModel.onPasswordChange("password123")
    }

    // Testy stanu początkowego

    @Test
    fun initialState_emptyData() {

        val state = viewModel.uiState.value

        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    // Testy aktualizacji formularza

    @Test
    fun emailChange_updatesState() {

        viewModel.onEmailChange("user@gmail.com")

        assertEquals(
            "user@gmail.com",
            viewModel.uiState.value.email
        )
    }

    @Test
    fun passwordChange_updatesState() {

        viewModel.onPasswordChange("password123")

        assertEquals(
            "password123",
            viewModel.uiState.value.password
        )
    }

    @Test
    fun emailChange_clearsError() {

        viewModel.login {}

        assertNotNull(viewModel.uiState.value.error)

        viewModel.onEmailChange("user@gmail.com")

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun passwordChange_clearsError() {

        viewModel.login {}

        assertNotNull(viewModel.uiState.value.error)

        viewModel.onPasswordChange("password123")

        assertNull(viewModel.uiState.value.error)
    }

    // Testy walidacji logowania

    @Test
    fun login_emptyData() {

        viewModel.login {}

        assertEquals(
            "Wpisz email i hasło.",
            viewModel.uiState.value.error
        )

        assertEquals(0, repository.loginCalls)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun login_emptyPassword() {

        viewModel.onEmailChange("user@gmail.com")

        viewModel.login {}

        assertEquals(
            "Wpisz email i hasło.",
            viewModel.uiState.value.error
        )

        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun login_invalidEmail() {

        viewModel.onEmailChange("invalid-email")
        viewModel.onPasswordChange("password123")

        viewModel.login {}

        assertEquals(
            "Podaj poprawny adres email.",
            viewModel.uiState.value.error
        )

        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun login_shortPasswordIsAllowed() {

        viewModel.onEmailChange("user@gmail.com")
        viewModel.onPasswordChange("123")

        viewModel.login {}

        assertEquals(1, repository.loginCalls)
        assertTrue(viewModel.uiState.value.isLoading)
    }

    // Testy przebiegu logowania

    @Test
    fun login_setsLoadingState() {

        enterValidData()

        viewModel.login {}

        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        assertEquals(1, repository.loginCalls)
    }

    @Test
    fun login_successClearsState() {

        enterValidData()

        viewModel.login {}

        repository.completeSuccess()

        assertEquals(
            AuthUiState(),
            viewModel.uiState.value
        )
    }

    @Test
    fun login_successCallsCallback() {

        enterValidData()

        var successCalls = 0

        viewModel.login {
            successCalls++
        }

        assertEquals(0, successCalls)

        repository.completeSuccess()

        assertEquals(1, successCalls)
    }

    @Test
    fun login_errorUpdatesState() {

        enterValidData()

        viewModel.login {}

        repository.completeError(
            "Nieprawidłowy email lub hasło."
        )

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)

        assertEquals(
            "Nieprawidłowy email lub hasło.",
            state.error
        )
    }

    @Test
    fun login_errorDoesNotCallSuccess() {

        enterValidData()

        var success = false

        viewModel.login {
            success = true
        }

        repository.completeError("Błąd logowania.")

        assertFalse(success)
    }

    @Test
    fun login_errorWithoutMessage() {

        enterValidData()

        viewModel.login {}

        repository.completeError(null)

        assertEquals(
            "Wystąpił nieoczekiwany błąd.",
            viewModel.uiState.value.error
        )

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun login_blocksRepeatedRequest() {

        enterValidData()

        viewModel.login {}
        viewModel.login {}
        viewModel.login {}

        assertEquals(1, repository.loginCalls)
    }

    @Test
    fun login_retryAfterError() {

        enterValidData()

        viewModel.login {}

        repository.completeError("Błąd logowania.")

        viewModel.login {}

        assertEquals(2, repository.loginCalls)
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun login_normalizesEmail() {

        viewModel.onEmailChange("  user@gmail.com  ")
        viewModel.onPasswordChange("password123")

        viewModel.login {}

        assertEquals(
            "user@gmail.com",
            repository.lastEmail
        )
    }

    @Test
    fun login_preservesPasswordSpaces() {

        viewModel.onEmailChange("user@gmail.com")
        viewModel.onPasswordChange(" password123 ")

        viewModel.login {}

        assertEquals(
            " password123 ",
            repository.lastPassword
        )
    }

    // Testy rejestracji

    @Test
    fun register_shortPassword() {

        viewModel.onEmailChange("user@gmail.com")
        viewModel.onPasswordChange("12345")

        viewModel.register {}

        assertEquals(
            "Hasło musi mieć minimum 6 znaków.",
            viewModel.uiState.value.error
        )

        assertEquals(0, repository.registerCalls)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun register_minimumPasswordLength() {

        viewModel.onEmailChange("user@gmail.com")
        viewModel.onPasswordChange("123456")

        viewModel.register {}

        assertEquals(1, repository.registerCalls)
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun register_success() {

        enterValidData()

        var successCalls = 0

        viewModel.register {
            successCalls++
        }

        assertTrue(viewModel.uiState.value.isLoading)

        repository.completeSuccess()

        assertEquals(1, successCalls)

        assertEquals(
            AuthUiState(),
            viewModel.uiState.value
        )
    }

    @Test
    fun register_error() {

        enterValidData()

        var success = false

        viewModel.register {
            success = true
        }

        repository.completeError(
            "Konto z tym adresem email już istnieje."
        )

        assertFalse(success)
        assertFalse(viewModel.uiState.value.isLoading)

        assertEquals(
            "Konto z tym adresem email już istnieje.",
            viewModel.uiState.value.error
        )
    }

    @Test
    fun register_blocksRepeatedRequest() {

        enterValidData()

        viewModel.register {}
        viewModel.register {}

        assertEquals(1, repository.registerCalls)
    }

    @Test
    fun auth_blocksRegisterDuringLogin() {

        enterValidData()

        viewModel.login {}
        viewModel.register {}

        assertEquals(1, repository.loginCalls)
        assertEquals(0, repository.registerCalls)
    }
}
