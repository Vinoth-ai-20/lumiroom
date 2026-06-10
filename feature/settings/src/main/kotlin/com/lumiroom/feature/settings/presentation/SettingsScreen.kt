package com.lumiroom.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Theme
            Text("Appearance", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            ListItem(
                headlineContent = { Text("Theme") },
                supportingContent = { Text(prefs.themeMode) },
                trailingContent = {
                    // Cycle through LIGHT / DARK / SYSTEM on tap
                    TextButton(onClick = {
                        val next = when (prefs.themeMode) {
                            "LIGHT" -> "DARK"; "DARK" -> "SYSTEM"; else -> "LIGHT"
                        }
                        viewModel.setThemeMode(next)
                    }) { Text("Change") }
                }
            )

            HorizontalDivider()

            // AR Settings
            Text("AR Settings", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            ListItem(
                headlineContent = { Text("Show AR Planes") },
                trailingContent = {
                    Switch(checked = prefs.arPlaneDisplay,
                        onCheckedChange = viewModel::setArPlaneDisplay)
                }
            )
            ListItem(
                headlineContent = { Text("Voice Commands") },
                trailingContent = {
                    Switch(checked = prefs.voiceCommandsEnabled,
                        onCheckedChange = viewModel::setVoiceCommandsEnabled)
                }
            )

            HorizontalDivider()

            // Cloud Dashboard
            Text("Cloud Dashboard", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            ListItem(
                headlineContent = { Text("Auto Sync") },
                trailingContent = {
                    Switch(checked = prefs.autoSyncEnabled,
                        onCheckedChange = viewModel::setAutoSyncEnabled)
                }
            )
            ListItem(
                headlineContent = { Text("Manual Backup") },
                modifier = Modifier.clickable { viewModel.syncNow() },
                supportingContent = { Text("Sync rooms to cloud now") },
                trailingContent = {
                    Icon(androidx.compose.material.icons.Icons.Default.Refresh, contentDescription = "Sync")
                }
            )

            HorizontalDivider()

            // Account
            Text("Account", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            ListItem(
                headlineContent = { Text("Sign Out") },
                modifier = Modifier.clickable { onSignOut() },
            )
        }
    }
}
