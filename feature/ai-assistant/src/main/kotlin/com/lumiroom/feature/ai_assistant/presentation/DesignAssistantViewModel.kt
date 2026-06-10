package com.lumiroom.feature.ai_assistant.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiroom.core.database.dao.RoomDesignDao
import com.lumiroom.core.database.dao.FurnitureDao
import com.lumiroom.core.database.entity.FurnitureEntity
import com.lumiroom.core.room_analysis.RoomScoreEngine
import com.lumiroom.core.room_analysis.LayoutOptimizationEngine
import com.lumiroom.core.room_analysis.BudgetPlanner
import com.lumiroom.core.room_analysis.RoomHealthResult
import com.lumiroom.core.room_analysis.BudgetPlan
import com.lumiroom.core.recommendation.RecommendationEngine
import com.lumiroom.core.room_analysis.RoomStyle
import com.lumiroom.feature.ai_assistant.data.DesignAssistantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(val text: String, val isUser: Boolean)

data class AiAssistantUiState(
    val messages: List<ChatMessage> = listOf(ChatMessage("Hi! I'm Lumi, your design assistant. How can I help?", false)),
    val isLoading: Boolean = false,
    val roomHealth: RoomHealthResult? = null,
    val budgetPlan: BudgetPlan? = null,
    val recommendations: List<FurnitureEntity> = emptyList(),
    val layoutSuggestions: List<String> = emptyList()
)

@HiltViewModel
class DesignAssistantViewModel @Inject constructor(
    private val repository: DesignAssistantRepository,
    private val roomDao: RoomDesignDao,
    private val furnitureDao: FurnitureDao,
    private val roomScoreEngine: RoomScoreEngine,
    private val layoutOptimizationEngine: LayoutOptimizationEngine,
    private val budgetPlanner: BudgetPlanner,
    private val recommendationEngine: RecommendationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    private var currentRoomId: String? = null
    private var currentContextString = ""

    fun loadRoomContext(roomId: String?) {
        currentRoomId = roomId
        if (roomId == null) return

        viewModelScope.launch {
            roomDao.getRoomWithItems(roomId).collect { roomWithItems ->
                if (roomWithItems != null) {
                    val itemsWithFurniture = roomWithItems.placedItems.mapNotNull {
                        val f = furnitureDao.getFurnitureByIdOnce(it.furnitureId)
                        if (f != null) com.lumiroom.core.database.relation.PlacedItemWithFurniture(it, f) else null
                    }
                    
                    // Generate Health Score
                    val health = roomScoreEngine.calculateScore(itemsWithFurniture)
                    
                    // Generate Layout Suggestions
                    val suggestions = layoutOptimizationEngine.analyzeLayout(itemsWithFurniture)
                    
                    // Generate Budget (Target Budget hardcoded to 2000 for now, could be dynamic)
                    val furnitureList = itemsWithFurniture.map { it.furniture }
                    val budget = budgetPlanner.calculateBudgetPlan(furnitureList, 2000.0)

                    // Generate Recommendations
                    val catalog = furnitureDao.getAllFurniture().first()
                    val targetStyle = try { RoomStyle.valueOf(roomWithItems.roomDesign.styleTag ?: "UNKNOWN") } catch(e: Exception) { RoomStyle.UNKNOWN }
                    val recs = recommendationEngine.getGeneralRecommendations(catalog, targetStyle, furnitureList)

                    _uiState.value = _uiState.value.copy(
                        roomHealth = health,
                        budgetPlan = budget,
                        layoutSuggestions = suggestions,
                        recommendations = recs
                    )

                    // Build string for LLM context
                    currentContextString = """
                        The user is asking about a room named "${roomWithItems.roomDesign.name}".
                        The layout efficiency score is ${health.layoutEfficiency}/100.
                        They have placed ${furnitureList.size} items: ${furnitureList.joinToString { it.name }}.
                        The estimated cost is ${budget.currentCost}.
                    """.trimIndent()
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text, true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isLoading = true
        )

        viewModelScope.launch {
            try {
                var modelReply = ""
                val modelMessageIndex = _uiState.value.messages.size
                
                // Add empty placeholder for streaming
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + ChatMessage("", false)
                )

                repository.sendMessageStream(text, currentContextString).collect { chunk ->
                    modelReply += chunk.text ?: ""
                    val updatedMessages = _uiState.value.messages.toMutableList()
                    updatedMessages[modelMessageIndex] = ChatMessage(modelReply, false)
                    _uiState.value = _uiState.value.copy(messages = updatedMessages)
                }
            } catch (e: Exception) {
                val updatedMessages = _uiState.value.messages.toMutableList()
                updatedMessages.add(ChatMessage("Sorry, I couldn't connect to my AI brain. Please check your internet connection.", false))
                _uiState.value = _uiState.value.copy(messages = updatedMessages)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
