package com.lumiroom.feature.ai_assistant.data.ai

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LumiDesignAssistant @Inject constructor() {

    private val remoteConfig = Firebase.remoteConfig.apply {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        setConfigSettingsAsync(configSettings)
        setDefaultsAsync(
            mapOf(
                "ai_model_name" to "gemini-2.5-flash",
                "ai_system_prompt" to LumiSystemPrompt.DEFAULT_PROMPT,
                "ai_temperature" to 0.7,
                "ai_top_p" to 0.95,
                "ai_top_k" to 40,
                "ai_max_tokens" to 1024
            )
        )
    }

    private var chatSession: com.google.firebase.ai.Chat? = null

    suspend fun initialize() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            // Fallback to defaults
        }

        val modelName = remoteConfig.getString("ai_model_name")
        val systemPrompt = remoteConfig.getString("ai_system_prompt")
        val temp = remoteConfig.getDouble("ai_temperature").toFloat()
        val p = remoteConfig.getDouble("ai_top_p").toFloat()
        val k = remoteConfig.getLong("ai_top_k").toInt()
        val tokens = remoteConfig.getLong("ai_max_tokens").toInt()

        val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = modelName,
            systemInstruction = content { text(systemPrompt) },
            generationConfig = generationConfig {
                temperature = temp
                topP = p
                topK = k
                maxOutputTokens = tokens
            }
        )

        chatSession = generativeModel.startChat()
    }

    fun sendMessageStream(prompt: String): Flow<String> = flow {
        if (chatSession == null) {
            initialize()
        }
        
        val chat = chatSession ?: throw IllegalStateException("Chat session not initialized")
        
        val responseStream = chat.sendMessageStream(prompt)
        responseStream.collect { chunk ->
            chunk.text?.let { text ->
                emit(text)
            }
        }
    }
    
    fun clearHistory() {
        chatSession = null
    }
}
