package com.lumiroom.feature.voice.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceHelpDialog(
    onDismiss: () -> Unit
) {
    val commandCategories = listOf(
        "Placement" to listOf("Place sofa", "Add a chair", "Put the modern table"),
        "Selection" to listOf("Select sofa", "Select the last item", "Deselect item"),
        "Manipulation" to listOf("Rotate right", "Rotate left 45 degrees", "Scale up", "Move forward"),
        "Editing" to listOf("Duplicate selected", "Delete selected", "Replace with chair"),
        "Room Management" to listOf("Save room", "Load room", "Create new room"),
        "Catalog" to listOf("Open catalog", "Show sofas", "Search modern sofa"),
        "View Controls" to listOf("Show planes", "Hide planes", "Focus selected", "Reset camera"),
        "Capture" to listOf("Take a screenshot", "Share screenshot")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Voice Commands") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(commandCategories) { (category, commands) ->
                    Column {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        commands.forEach { cmd ->
                            Text(
                                text = "• \"$cmd\"",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}
