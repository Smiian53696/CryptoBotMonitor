package com.example.cryptobotmonitor.presentation.auth

import androidx.lifecycle.ViewModel
import com.example.cryptobotmonitor.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(email = email, error = null)
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(password = password, error = null)
        }
    }

    fun login(onSuccess: () -> Unit) {
        val email = uiState.value.email
        val password = uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.update {
                it.copy(error = "Wpisz email i hasło.")
            }
            return
        }

        _uiState.update {
            it.copy(isLoading = true, error = null)
        }

        repository.login(email, password) { success, message ->
            if (success) {
                _uiState.update {
                    it.copy(isLoading = false, error = null)
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = message)
                }
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        val email = uiState.value.email
        val password = uiState.value.password

        if (email.isBlank() || password.isBlank()) {
            _uiState.update {
                it.copy(error = "Wpisz email i hasło.")
            }
            return
        }

        if (password.length < 6) {
            _uiState.update {
                it.copy(error = "Hasło musi mieć minimum 6 znaków.")
            }
            return
        }

        _uiState.update {
            it.copy(isLoading = true, error = null)
        }

        repository.register(email, password) { success, message ->
            if (success) {
                _uiState.update {
                    it.copy(isLoading = false, error = null)
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(isLoading = false, error = message)
                }
            }
        }
    }
}

