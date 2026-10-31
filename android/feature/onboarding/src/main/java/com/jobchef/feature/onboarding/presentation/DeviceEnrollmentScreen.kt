package com.jobchef.feature.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun DeviceEnrollmentScreen(
    state: DeviceEnrollmentState,
    onRegister: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Register this device", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Job Chef uses a private, revocable device identity. There is no account login or cross-device recovery.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
        )
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.take(200) },
            label = { Text("One-time enrollment code") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !state.isRegistering,
            modifier = Modifier.fillMaxWidth()
        )
        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Button(
            onClick = { onRegister(code) },
            enabled = !state.isRegistering && code.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            if (state.isRegistering) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            } else {
                Text("Register device")
            }
        }
    }
}
