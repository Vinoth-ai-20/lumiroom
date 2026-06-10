package com.lumiroom.feature.ai_assistant.data.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

object AIHealthCheck {
    private const val TAG = "AIHealthCheck"

    suspend fun performCheck() {
        Log.d(TAG, "=== Starting AI Health Check ===")
        try {
            val latency = measureTimeMillis {
                withContext(Dispatchers.IO) {
                    val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
                        .generativeModel(
                            modelName = "gemini-2.5-flash"
                        )
                    
                    Log.d(TAG, "Generating content for prompt: 'Hello'")
                    val response = generativeModel.generateContent("Hello")
                    Log.d(TAG, "Response received: ${response.text}")
                }
            }
            Log.d(TAG, "=== AI Health Check Passed in ${latency}ms ===")
        } catch (e: Exception) {
            Log.e(TAG, "=== AI Health Check Failed ===")
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Message: ${e.message}")
            Log.e(TAG, "Cause: ${e.cause}")
            Log.e(TAG, "Stack trace:", e)
        }
    }
}
