package com.lumiroom.feature.ai_assistant.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumiroom.core.room_analysis.RoomHealthCategory
import com.lumiroom.core.ui.theme.LumiroomPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    roomDesignId: String?,
    onNavigateToCatalog: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: DesignAssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(roomDesignId) {
        viewModel.loadRoomContext(roomDesignId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lumi Design Assistant") },
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
        ) {
            // Room Health Dashboard & Recommendations
            if (uiState.roomHealth != null) {
                DashboardHeader(
                    uiState = uiState,
                    onNavigateToCatalog = onNavigateToCatalog
                )
            }

            // Chat Messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                items(uiState.messages) { msg ->
                    ChatBubble(msg)
                }
                if (uiState.isLoading) {
                    item {
                        Text(
                            text = "Lumi is thinking...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            // Input Area
            ChatInput(
                onSendMessage = { viewModel.sendMessage(it) },
                enabled = !uiState.isLoading
            )
        }
    }
}

@Composable
fun DashboardHeader(
    uiState: AiAssistantUiState,
    onNavigateToCatalog: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val health = uiState.roomHealth!!
            Text("Room Health: ${health.score}/100 (${health.category.name})", style = MaterialTheme.typography.titleMedium)
            
            uiState.budgetPlan?.let { budget ->
                Spacer(modifier = Modifier.height(4.dp))
                Text("Budget Used: ₹${budget.currentCost}", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(
                    progress = { budget.utilizationPercentage },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }

            if (uiState.layoutSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Layout Suggestions:", style = MaterialTheme.typography.labelMedium)
                uiState.layoutSuggestions.take(2).forEach {
                    Text("• $it", style = MaterialTheme.typography.bodySmall)
                }
            }

            if (uiState.recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Recommended Furniture:", style = MaterialTheme.typography.labelMedium)
                Text(uiState.recommendations.joinToString { it.name }, style = MaterialTheme.typography.bodySmall)
                
                val context = androidx.compose.ui.platform.LocalContext.current
                Row {
                    TextButton(onClick = onNavigateToCatalog) {
                        Text("Browse Catalog")
                    }
                    TextButton(onClick = {
                        val report = """
                            Lumiroom Insight Report
                            -----------------------
                            Room Health: ${health.score}/100 (${health.category.name})
                            Budget Used: ₹${uiState.budgetPlan?.currentCost ?: 0.0}
                            
                            Layout Suggestions:
                            ${uiState.layoutSuggestions.joinToString("\n") { "- $it" }}
                            
                            Recommended Additions:
                            ${uiState.recommendations.joinToString("\n") { "- ${it.name}" }}
                        """.trimIndent()
                        
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, report)
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }) {
                        Text("Export Report")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (msg.isUser) LumiroomPrimary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (msg.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = alignment) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(bgColor)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(text = msg.text, color = textColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ChatInput(onSendMessage: (String) -> Unit, enabled: Boolean) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask Lumi about your room...") },
            enabled = enabled,
            maxLines = 3
        )
        IconButton(
            onClick = {
                onSendMessage(text)
                text = ""
            },
            enabled = enabled && text.isNotBlank()
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}
