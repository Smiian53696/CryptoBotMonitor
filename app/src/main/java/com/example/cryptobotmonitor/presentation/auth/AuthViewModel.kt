
package com.example.cryptobotmonitor.presentation.auth
import com.example.cryptobotmonitor.data.auth.AuthRepositoryInterface
import com.example.cryptobotmonitor.validation.AuthValidator
import androidx.lifecycle.ViewModel
import com.example.cryptobotmonitor.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val repository: AuthRepositoryInterface = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Aktualizacja adresu email
    fun onEmailChange(email: String) {

        _uiState.update {
            it.copy(email = email, error = null)
        }
    }

    // Aktualizacja hasła
    fun onPasswordChange(password: String) {

        _uiState.update {
            it.copy(password = password, error = null)
        }
    }

    // Sprawdzanie poprawności danych

    // Sprawdzanie poprawności danych
    private fun validateData(
        email: String,
        password: String,
        isRegister: Boolean
    ): Boolean {

        val error = AuthValidator.validateData(
            email = email,
            password = password,
            isRegister = isRegister
        )

        if (error != null) {

            _uiState.update {
                it.copy(error = error)
            }

            return false
        }

        return true
    }


    // Logowanie użytkownika
    fun login(onSuccess: () -> Unit) {

        // Blokada kolejnego zapytania
        if (uiState.value.isLoading) return

        val email = AuthValidator.normalizeEmail(
            uiState.value.email
        )
        val password = uiState.value.password

        if (!validateData(email, password, false)) {
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                error = null
            )
        }

        repository.login(email, password) { success, message ->

            handleAuthResult(
                success,
                message,
                onSuccess
            )
        }
    }

    // Rejestracja użytkownika
    fun register(onSuccess: () -> Unit) {

        // Blokada kolejnego zapytania
        if (uiState.value.isLoading) return

        val email = AuthValidator.normalizeEmail(
            uiState.value.email
        )
        val password = uiState.value.password

        if (!validateData(email, password, true)) {
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                error = null
            )
        }

        repository.register(email, password) { success, message ->

            handleAuthResult(
                success,
                message,
                onSuccess
            )
        }
    }

    // Obsługa wyniku logowania i rejestracji
    private fun handleAuthResult(
        success: Boolean,
        message: String?,
        onSuccess: () -> Unit
    ) {

        if (success) {

            // Usunięcie danych formularza po zalogowaniu
            _uiState.value = AuthUiState()

            onSuccess()

        } else {

            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = message ?: "Wystąpił nieoczekiwany błąd."
                )
            }
        }
    }
}
