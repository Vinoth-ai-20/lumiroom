package com.lumiroom.feature.ai_assistant.presentation.chat

import com.lumiroom.feature.ai_assistant.domain.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val errorMessage: String? = null
)
