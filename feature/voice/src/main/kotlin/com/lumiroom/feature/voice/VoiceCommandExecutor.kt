package com.lumiroom.feature.voice

/**
 * Interface that components (like ViewModels) implement to handle Voice Commands
 * dispatched from the Voice System.
 */
interface VoiceCommandExecutor {
    fun executeCommand(command: VoiceCommand)
}
