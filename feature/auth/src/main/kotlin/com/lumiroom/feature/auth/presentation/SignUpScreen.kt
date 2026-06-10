package com.lumiroom.feature.auth.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumiroom.core.ui.theme.LumiroomPrimary

@Composable
fun SignUpScreen(
    onNavigateToMain: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState.isAuthenticated) { if (uiState.isAuthenticated) onNavigateToMain() }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))

        var displayName by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        OutlinedTextField(value = displayName, onValueChange = { displayName = it },
            label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it },
            label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it },
            label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
        Spacer(Modifier.height(24.dp))

        Button(onClick = { viewModel.signUp(email, password, displayName) },
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = LumiroomPrimary)) {
            Text("Create Account", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(8.dp))
        val context = androidx.compose.ui.platform.LocalContext.current
        OutlinedButton(onClick = { viewModel.signInWithGoogle(context) },
            modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.large) {
            Text("Sign in with Google", style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onClick = onNavigateBack) { Text("Already have an account? Sign In") }

        uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
