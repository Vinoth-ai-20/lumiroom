package com.lumiroom.feature.ai_assistant.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumiroom.core.ui.theme.LumiroomPrimary
import com.lumiroom.feature.ai_assistant.domain.model.ChatMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Lumi Design Assistant", style = MaterialTheme.typography.titleMedium)
                        Text("Your Personal Interior Designer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearConversation() }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear Conversation")
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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item {
                        SuggestedChips { query ->
                            viewModel.sendMessage(query)
                        }
                    }
                }
                
                items(uiState.messages) { msg ->
                    ChatBubble(
                        msg = msg,
                        onRetry = { viewModel.retryLastMessage() }
                    )
                }
                
                if (uiState.isTyping && uiState.messages.lastOrNull()?.isStreaming != true) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            AnimatedVisibility(visible = uiState.errorMessage != null && !uiState.isTyping) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Error",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        IconButton(onClick = { viewModel.retryLastMessage() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Retry", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            ChatInput(
                onSendMessage = { viewModel.sendMessage(it) },
                enabled = !uiState.isTyping
            )
        }
    }
}

@Composable
fun SuggestedChips(onChipClick: (String) -> Unit) {
    val suggestions = listOf(
        "Suggest furniture for a small bedroom",
        "How can I make my room look modern?",
        "Recommend colors for my living room",
        "How should I arrange my sofa?",
        "What furniture suits Scandinavian style?"
    )
    
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text("Suggestions", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestions) { text ->
                AssistChip(
                    onClick = { onChipClick(text) },
                    label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage, onRetry: () -> Unit) {
    val isUser = msg.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (isUser) LumiroomPrimary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isUser) RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (msg.isError) MaterialTheme.colorScheme.errorContainer else bgColor)
                    .padding(12.dp)
                    .widthIn(max = 280.dp)
            ) {
                Text(
                    text = if (msg.text.isEmpty() && msg.isStreaming) "Thinking..." else msg.text, 
                    color = if (msg.isError) MaterialTheme.colorScheme.onErrorContainer else textColor, 
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Text(text = "Thinking...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ChatInput(onSendMessage: (String) -> Unit, enabled: Boolean) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message Lumi...") },
            enabled = enabled,
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = LumiroomPrimary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                onSendMessage(text)
                text = ""
            },
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier.background(
                color = if (enabled && text.isNotBlank()) LumiroomPrimary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(24.dp)
            )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send, 
                contentDescription = "Send",
                tint = if (enabled && text.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
