package com.lumiroom.feature.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Android [SpeechRecognizer] lifecycle and exposes recognition
 * results as a cold [Flow].
 *
 * Usage:
 * ```kotlin
 * voiceCommandManager.recognitionResults
 *     .collect { result -> commandParser.parse(result) }
 * ```
 *
 * Only one recognition session can be active at a time. Call [startListening]
 * to begin a session and [stopListening] to end it early.
 */
@Singleton
class VoiceCommandManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var speechRecognizer: SpeechRecognizer? = null

    /**
     * Emits [VoiceResult] events from the active SpeechRecognizer session.
     * The flow is cold — recognition starts when collected and stops when cancelled.
     */
    fun startRecognition(): Flow<VoiceResult> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(VoiceResult.Error("Speech recognition not available on this device"))
            close()
            return@callbackFlow
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    trySend(VoiceResult.Listening)
                }
                override fun onBeginningOfSpeech() {
                    trySend(VoiceResult.Speaking)
                }
                override fun onRmsChanged(rmsdB: Float) {
                    trySend(VoiceResult.RmsChanged(rmsdB))
                }
                override fun onResults(results: Bundle?) {
                    val matches = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?: return
                    val transcript = matches.firstOrNull() ?: return
                    trySend(VoiceResult.Transcript(transcript))
                    close()
                }
                override fun onError(error: Int) {
                    trySend(VoiceResult.Error(mapSpeechError(error)))
                    close()
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull() ?: return
                    trySend(VoiceResult.Partial(partial))
                }
                override fun onEndOfSpeech() {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
            )
        }

        awaitClose {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    private fun mapSpeechError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO            -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT           -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission not granted"
        SpeechRecognizer.ERROR_NETWORK          -> "Network error — check connection"
        SpeechRecognizer.ERROR_NO_MATCH         -> "No speech recognized"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY  -> "Recognizer busy"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT   -> "No speech input detected"
        else                                    -> "Unknown speech error ($error)"
    }
}

/** Sealed class representing all possible states from the speech recognizer. */
sealed class VoiceResult {
    /** Microphone open, awaiting speech. */
    object Listening : VoiceResult()

    /** User has begun speaking. */
    object Speaking : VoiceResult()

    /** RMS dB value for waveform visualization. */
    data class RmsChanged(val rmsdB: Float) : VoiceResult()

    /** Partial transcript as user speaks (for real-time display). */
    data class Partial(val text: String) : VoiceResult()

    /** Final committed transcript. */
    data class Transcript(val text: String) : VoiceResult()

    /** Recognition failure. */
    data class Error(val message: String) : VoiceResult()
}
