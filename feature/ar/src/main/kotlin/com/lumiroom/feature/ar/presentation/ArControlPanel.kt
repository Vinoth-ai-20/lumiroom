package com.lumiroom.feature.ar.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lumiroom.core.ui.theme.LumiroomGlassMedium
import com.lumiroom.core.ui.theme.LumiroomPrimary

/**
 * Floating AR control panel shown at the bottom of the AR scene.
 * Contains undo/redo, catalog open, and room planner toggles.
 */
@Composable
fun ArControlPanel(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenPlanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(Icons.Default.Undo, "Undo",
                    tint = if (canUndo) LumiroomPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(Icons.Default.Redo, "Redo",
                    tint = if (canRedo) LumiroomPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }

            FilledTonalButton(
                onClick = onOpenCatalog,
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Furniture", style = MaterialTheme.typography.labelMedium)
            }

            IconButton(onClick = onOpenPlanner) {
                Icon(Icons.Default.GridView, "2D Planner")
            }
        }
    }
}
