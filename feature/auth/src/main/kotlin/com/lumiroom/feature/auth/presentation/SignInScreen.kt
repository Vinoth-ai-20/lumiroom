package com.lumiroom.feature.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumiroom.core.ui.theme.LumiroomBackground
import com.lumiroom.core.ui.theme.LumiroomPrimary

/**
 * Sign In screen.
 * Full implementation in Milestone 1 — this scaffold wires navigation correctly.
 */
@Composable
fun SignInScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onNavigateToMain()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LumiroomBackground, LumiroomBackground))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Welcome to Lumiroom", style = MaterialTheme.typography.headlineMedium)
            Text("Sign in to continue", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(16.dp))

            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )

            Button(
                onClick = { viewModel.signIn(email, password) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = LumiroomPrimary),
                enabled = !uiState.isLoading,
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Sign In", style = MaterialTheme.typography.labelLarge)
                }
            }

            val context = androidx.compose.ui.platform.LocalContext.current
            OutlinedButton(
                onClick = { viewModel.signInWithGoogle(context) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = !uiState.isLoading,
            ) {
                Text("Sign in with Google", style = MaterialTheme.typography.labelLarge)
            }

            TextButton(onClick = onNavigateToSignUp) {
                Text("Don't have an account? Sign Up")
            }

            TextButton(onClick = { viewModel.signInAsGuest() }) {
                Text("Continue as Guest", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            uiState.errorMessage?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
