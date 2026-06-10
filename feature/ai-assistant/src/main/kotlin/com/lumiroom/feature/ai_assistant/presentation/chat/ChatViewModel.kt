package com.lumiroom.feature.ai_assistant.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumiroom.feature.ai_assistant.data.ai.LumiAssistantRepository
import com.lumiroom.feature.ai_assistant.domain.model.ChatMessage
import com.lumiroom.feature.ai_assistant.domain.model.MessageRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: LumiAssistantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text = text, role = MessageRole.USER)
        val initialAssistantMessage = ChatMessage(text = "", role = MessageRole.ASSISTANT, isStreaming = true)
        
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage + initialAssistantMessage,
                isTyping = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            repository.sendMessageStream(text)
                .onStart {
                    // Stream started, but state is already updated
                }
                .onCompletion { error ->
                    _uiState.update { state ->
                        state.copy(
                            isTyping = false,
                            messages = state.messages.map { msg ->
                                if (msg.id == initialAssistantMessage.id) {
                                    msg.copy(isStreaming = false, isError = error != null)
                                } else {
                                    msg
                                }
                            }
                        )
                    }
                }
                .catch { error ->
                    _uiState.update { state ->
                        state.copy(
                            errorMessage = error.message ?: "An unknown error occurred.",
                            messages = state.messages.map { msg ->
                                if (msg.id == initialAssistantMessage.id) {
                                    msg.copy(text = error.message ?: "Error", isError = true)
                                } else {
                                    msg
                                }
                            }
                        )
                    }
                }
                .collect { chunk ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == initialAssistantMessage.id) {
                                    msg.copy(text = msg.text + chunk)
                                } else {
                                    msg
                                }
                            }
                        )
                    }
                }
        }
    }

    fun clearConversation() {
        repository.clearConversation()
        _uiState.update { ChatUiState() }
    }

    fun retryLastMessage() {
        val lastUserMessage = _uiState.value.messages.lastOrNull { it.role == MessageRole.USER }
        if (lastUserMessage != null) {
            // Remove the failed assistant message
            _uiState.update { state ->
                state.copy(
                    messages = state.messages.dropLastWhile { it.role == MessageRole.ASSISTANT },
                    errorMessage = null
                )
            }
            // Send again without adding a new user bubble
            val initialAssistantMessage = ChatMessage(text = "", role = MessageRole.ASSISTANT, isStreaming = true)
            _uiState.update { state ->
                state.copy(
                    messages = state.messages + initialAssistantMessage,
                    isTyping = true
                )
            }
            
            viewModelScope.launch {
                repository.sendMessageStream(lastUserMessage.text)
                    .onCompletion { error ->
                        _uiState.update { state ->
                            state.copy(
                                isTyping = false,
                                messages = state.messages.map { msg ->
                                    if (msg.id == initialAssistantMessage.id) {
                                        msg.copy(isStreaming = false, isError = error != null)
                                    } else {
                                        msg
                                    }
                                }
                            )
                        }
                    }
                    .catch { error ->
                        _uiState.update { state ->
                            state.copy(
                                errorMessage = error.message ?: "An unknown error occurred.",
                                messages = state.messages.map { msg ->
                                    if (msg.id == initialAssistantMessage.id) {
                                        msg.copy(text = error.message ?: "Error", isError = true)
                                    } else {
                                        msg
                                    }
                                }
                            )
                        }
                    }
                    .collect { chunk ->
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.map { msg ->
                                    if (msg.id == initialAssistantMessage.id) {
                                        msg.copy(text = msg.text + chunk)
                                    } else {
                                        msg
                                    }
                                }
                            )
                        }
                    }
            }
        }
    }
}
