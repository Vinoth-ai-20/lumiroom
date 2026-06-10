package com.lumiroom.core.room_analysis

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

enum class RoomStyle {
    MODERN,
    MINIMALIST,
    SCANDINAVIAN,
    INDUSTRIAL,
    TRADITIONAL,
    CONTEMPORARY,
    BOHEMIAN,
    UNKNOWN
}

data class StyleAnalysisResult(
    val style: RoomStyle,
    val confidenceScore: Float, // 0.0 to 1.0
    val explanation: String
)

@Singleton
class RoomStyleAnalyzer @Inject constructor() {
    
    // We instantiate lazily so it doesn't crash if Vertex is not set up
    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel("gemini-1.5-flash")
    }

    /**
     * Analyzes the provided bitmap using Gemini 1.5 Flash to determine the interior design style.
     */
    suspend fun analyzeRoomStyle(bitmap: Bitmap): Result<StyleAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Analyze the interior design style of this room.
                Classify it into EXACTLY ONE of the following styles:
                MODERN, MINIMALIST, SCANDINAVIAN, INDUSTRIAL, TRADITIONAL, CONTEMPORARY, BOHEMIAN.
                
                Provide your response in the following strict format:
                STYLE: [Style Name]
                CONFIDENCE: [0.0 to 1.0]
                EXPLANATION: [1-2 sentences explaining why]
            """.trimIndent()

            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
            
            val responseText = response.text ?: return@withContext Result.failure(Exception("Empty response from AI"))
            
            val styleMatch = Regex("STYLE:\\s*(\\w+)").find(responseText)
            val confMatch = Regex("CONFIDENCE:\\s*([0-9.]+)").find(responseText)
            val expMatch = Regex("EXPLANATION:\\s*(.*)", RegexOption.DOT_MATCHES_ALL).find(responseText)

            val styleStr = styleMatch?.groupValues?.get(1)?.uppercase() ?: "UNKNOWN"
            val style = try {
                RoomStyle.valueOf(styleStr)
            } catch (e: Exception) {
                RoomStyle.UNKNOWN
            }
            
            val confidence = confMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0.5f
            val explanation = expMatch?.groupValues?.get(1)?.trim() ?: "Could not parse explanation."

            Result.success(StyleAnalysisResult(style, confidence, explanation))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
