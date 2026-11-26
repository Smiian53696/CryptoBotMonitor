
package com.example.cryptobotmonitor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.cryptobotmonitor.ui.theme.*

// Uniwersalne pole tekstowe
@Composable
fun CryptoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    var passwordVisible by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Etykieta nad polem
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = CryptoTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            isError = isError,
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge,

            placeholder = {
                Text(
                    text = placeholder,
                    color = CryptoTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },

            // Ukrywanie lub pokazywanie hasła
            visualTransformation = if (
                isPassword && !passwordVisible
            ) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },

            trailingIcon = if (isPassword) {
                {
                    TextButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        },
                        enabled = enabled
                    ) {
                        Text(
                            text = if (passwordVisible) {
                                "Ukryj"
                            } else {
                                "Pokaż"
                            },
                            color = CryptoAccent,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            } else {
                trailingIcon
            },

            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,

            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CryptoSurface,
                unfocusedContainerColor = CryptoSurface,
                disabledContainerColor = CryptoSurface,
                errorContainerColor = CryptoSurface,

                focusedTextColor = CryptoTextPrimary,
                unfocusedTextColor = CryptoTextPrimary,

                focusedBorderColor = CryptoAccent,
                unfocusedBorderColor = CryptoBorder,

                cursorColor = CryptoAccent,
                errorBorderColor = CryptoNegative,
                errorCursorColor = CryptoNegative
            )
        )

        // Komunikat o błędzie
        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = errorMessage,
                color = CryptoNegative,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
