package com.lumiroom.feature.ai_assistant.data.ai

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton
import java.net.UnknownHostException
import java.net.SocketTimeoutException

@Singleton
class LumiAssistantRepository @Inject constructor(
    private val assistant: LumiDesignAssistant
) {
    companion object {
        private const val TAG = "LumiAssistantRepository"
    }

    fun sendMessageStream(prompt: String): Flow<String> {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Sending prompt length: ${prompt.length} chars")

        return assistant.sendMessageStream(prompt)
            .onStart {
                Log.d(TAG, "Stream started")
            }
            .onCompletion { error ->
                val duration = System.currentTimeMillis() - startTime
                if (error != null) {
                    Log.e(TAG, "Stream failed after $duration ms", error)
                } else {
                    Log.d(TAG, "Stream completed successfully in $duration ms")
                }
            }
            .catch { e ->
                Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Message: ${e.message}")
                Log.e(TAG, "Cause: ${e.cause}")
                Log.e(TAG, "Stack trace:", e)
                
                val friendlyMessage = when {
                    e is UnknownHostException -> "Unable to connect to Lumi AI. Please check your internet connection."
                    e is SocketTimeoutException -> "Lumi AI took too long to respond."
                    e is IllegalArgumentException -> "Lumi AI is not configured correctly."
                    e.javaClass.simpleName == "ServerException" -> {
                        val msg = e.message ?: ""
                        if (msg.contains("rate limit", ignoreCase = true) || msg.contains("429")) {
                            "Too many requests. Please try again later."
                        } else if (msg.contains("503") || msg.contains("unavailable", ignoreCase = true)) {
                            "Lumi AI is temporarily unavailable."
                        } else {
                            e.message ?: "An unknown server error occurred."
                        }
                    }
                    else -> e.message ?: "An unexpected error occurred."
                }
                
                throw Exception(friendlyMessage)
            }
    }

    fun clearConversation() {
        assistant.clearHistory()
        Log.d(TAG, "Conversation history cleared")
    }
}
