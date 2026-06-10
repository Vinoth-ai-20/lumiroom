package com.lumiroom.feature.ai_assistant.data

import com.google.firebase.vertexai.FirebaseVertexAI
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source wrapping Firebase Vertex AI (Gemini) for the AI Design Assistant.
 *
 * Model selection: Gemini 1.5 Flash — optimal balance of speed and quality for
 * conversational design suggestions. Flash is preferable here because:
 * - Interior design chat requires fast response cadence
 * - Streaming text rendering hides latency
 * - Context windows are moderate (catalog JSON ~10K tokens)
 *
 * To switch to Pro, change [MODEL_NAME] to "gemini-1.5-pro".
 */
@Singleton
class GeminiDataSource @Inject constructor() {

    companion object {
        private const val MODEL_NAME = "gemini-1.5-flash"

        /** System prompt injected on every request to ground the AI as a design assistant. */
        private const val SYSTEM_PROMPT = """
You are Lumi, an expert AI interior designer and furniture advisor embedded in the Lumiroom app.

Your capabilities:
- Suggest furniture combinations that match the user's room style and existing items
- Recommend specific furniture items from the provided catalog JSON
- Analyze room photos and describe the current design
- Answer questions about interior design, color theory, and furniture arrangements
- Provide measurements advice and spatial planning tips

Rules:
- Always recommend items that exist in the provided catalog context
- When recommending items, include their exact name and ID so the app can show them
- Keep responses concise and actionable (max 3 paragraphs unless asked for detail)
- Use friendly, enthusiastic language — you love great design
- Format furniture recommendations as: **[Item Name]** (ID: xxx) - brief reason
"""
    }

    private val vertexAI = FirebaseVertexAI.instance

    private val model = vertexAI.generativeModel(
        modelName = MODEL_NAME,
        generationConfig = generationConfig {
            temperature = 0.8f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 2048
        },
        systemInstruction = content { text(SYSTEM_PROMPT) },
    )

    /**
     * Sends a message and returns a streaming [Flow] of text chunk strings.
     *
     * @param userMessage The user's current message text
     * @param catalogContext JSON string of relevant furniture items for grounding
     * @param chatHistory List of prior [ChatMessage] for context continuity
     * @param roomContextJson Optional JSON describing current room dimensions + placed items
     */
    fun streamMessage(
        userMessage: String,
        catalogContext: String,
        chatHistory: List<ChatMessage>,
        roomContextJson: String? = null,
    ): Flow<String> {
        val chat = model.startChat(
            history = chatHistory.map { msg ->
                content(role = msg.role) { text(msg.content) }
            }
        )

        val fullPrompt = buildPrompt(userMessage, catalogContext, roomContextJson)

        return chat.sendMessageStream(fullPrompt)
            .map { response -> response.text ?: "" }
    }

    private fun buildPrompt(
        userMessage: String,
        catalogContext: String,
        roomContextJson: String?,
    ): String = buildString {
        if (roomContextJson != null) {
            appendLine("## Current Room Context")
            appendLine(roomContextJson)
            appendLine()
        }
        appendLine("## Available Furniture Catalog (relevant items)")
        appendLine(catalogContext)
        appendLine()
        appendLine("## User Message")
        appendLine(userMessage)
    }
}

/** A single chat message for history context. */
data class ChatMessage(
    val role: String,     // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)
