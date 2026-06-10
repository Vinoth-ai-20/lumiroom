package com.lumiroom.feature.ai_assistant.data

import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.type.GenerateContentResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DesignAssistantRepository @Inject constructor() {

    private val generativeModel by lazy {
        Firebase.vertexAI.generativeModel("gemini-1.5-flash")
    }

    private val chat by lazy {
        generativeModel.startChat(
            history = listOf(
                content(role = "user") { text("You are an expert interior design assistant. Your name is Lumi. You give concise, helpful advice about room layout, styling, and furniture recommendations.") },
                content(role = "model") { text("Hello! I am Lumi, your interior design assistant. How can I help you improve your space today?") }
            )
        )
    }

    fun sendMessageStream(message: String, contextString: String = ""): Flow<GenerateContentResponse> {
        val fullMessage = if (contextString.isNotEmpty()) {
            "Context: $contextString\nUser: $message"
        } else {
            message
        }
        return chat.sendMessageStream(fullMessage)
    }
}
