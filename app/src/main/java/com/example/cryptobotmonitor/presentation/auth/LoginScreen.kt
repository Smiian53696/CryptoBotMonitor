
package com.example.cryptobotmonitor.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cryptobotmonitor.ui.components.CryptoBrand
import com.example.cryptobotmonitor.ui.components.CryptoButton
import com.example.cryptobotmonitor.ui.components.CryptoTextField
import com.example.cryptobotmonitor.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Logowanie użytkownika
    val loginAction: () -> Unit = {
        if (!state.isLoading) {
            focusManager.clearFocus()
            viewModel.login(onSuccess = onLoginSuccess)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 24.dp,
                    vertical = 32.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Logo i nazwa aplikacji
            CryptoBrand()

            Spacer(modifier = Modifier.height(40.dp))

            // Nagłówek ekranu
            Text(
                text = "Witaj ponownie!",
                style = MaterialTheme.typography.headlineLarge,
                color = CryptoTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Zaloguj się, aby kontynuować",
                style = MaterialTheme.typography.bodyMedium,
                color = CryptoTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Pole adresu e-mail
            CryptoTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Adres e-mail",
                placeholder = "Wprowadź adres e-mail",
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(
                            androidx.compose.ui.focus.FocusDirection.Down
                        )
                    }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pole hasła
            CryptoTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Hasło",
                placeholder = "Wprowadź hasło",
                isPassword = true,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        loginAction()
                    }
                )
            )

            // Wyświetlenie błędu logowania
            state.error?.let { error ->

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = error,
                    color = CryptoNegative,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Przycisk logowania
            CryptoButton(
                text = "Zaloguj się",
                onClick = loginAction,
                enabled = !state.isLoading,
                isLoading = state.isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Przejście do rejestracji
            Text(
                text = "Nie masz jeszcze konta?",
                style = MaterialTheme.typography.bodyMedium,
                color = CryptoTextSecondary,
                textAlign = TextAlign.Center
            )

            TextButton(
                onClick = onRegisterClick,
                enabled = !state.isLoading
            ) {
                Text(
                    text = "Zarejestruj się",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (state.isLoading) {
                        CryptoTextSecondary
                    } else {
                        CryptoAccent
                    }
                )
            }
        }
    }
}
