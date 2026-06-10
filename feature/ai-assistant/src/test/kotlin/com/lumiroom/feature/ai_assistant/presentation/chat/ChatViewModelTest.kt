package com.lumiroom.feature.ai_assistant.presentation.chat

import com.lumiroom.feature.ai_assistant.data.ai.LumiAssistantRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: LumiAssistantRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        viewModel = ChatViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendMessage updates UI state correctly`() = runTest {
        val testResponse = "Hello, I am Lumi!"
        coEvery { repository.sendMessageStream(any()) } returns flowOf(testResponse)

        viewModel.sendMessage("Hi")
        
        // Let the coroutines execute
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("Hi", state.messages[0].text)
        assertEquals(testResponse, state.messages[1].text)
        assertEquals(false, state.isTyping)
        assertEquals(null, state.errorMessage)
    }
}
