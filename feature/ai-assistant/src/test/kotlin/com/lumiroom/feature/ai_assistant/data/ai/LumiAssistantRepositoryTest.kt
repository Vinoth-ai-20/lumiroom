package com.lumiroom.feature.ai_assistant.data.ai

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

class LumiAssistantRepositoryTest {

    private lateinit var assistant: LumiDesignAssistant
    private lateinit var repository: LumiAssistantRepository

    @Before
    fun setup() {
        assistant = mockk()
        repository = LumiAssistantRepository(assistant)
    }

    @Test
    fun `sendMessageStream maps UnknownHostException to friendly message`() = runTest {
        coEvery { assistant.sendMessageStream(any()) } returns flow {
            throw UnknownHostException("No internet")
        }

        var exceptionCaught = false
        try {
            repository.sendMessageStream("Test prompt").toList()
        } catch (e: Exception) {
            exceptionCaught = true
            assertTrue(e.message?.contains("offline") == true)
        }
        
        assertTrue(exceptionCaught)
    }
}
