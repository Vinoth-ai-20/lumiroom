package com.lumiroom.feature.ai_assistant.domain.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val isError: Boolean = false
) {
    val isUser: Boolean get() = role == MessageRole.USER
}
