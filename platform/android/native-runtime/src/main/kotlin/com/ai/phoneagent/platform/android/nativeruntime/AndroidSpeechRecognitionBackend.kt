package com.ai.phoneagent.platform.android.nativeruntime

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.core.capability.SpeechRecognitionEvent
import com.ai.phoneagent.core.capability.SpeechRecognitionRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidSpeechRecognitionBackend(
    private val context: Context,
) : SpeechRecognitionBackend {
    override val diagnostics: Map<String, String> = mapOf(
        "platform" to "android",
        "backend" to "speech-recognizer",
    )

    override fun recognize(request: SpeechRecognitionRequest): Flow<SpeechRecognitionEvent> = callbackFlow {
        val mainHandler = Handler(Looper.getMainLooper())
        var recognizer: SpeechRecognizer? = null
        mainHandler.post {
            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = speechRecognizer
            speechRecognizer.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit

                    override fun onPartialResults(partialResults: Bundle?) {
                        partialResults.firstText()?.let { trySend(SpeechRecognitionEvent.Partial(it)) }
                    }

                    override fun onResults(results: Bundle?) {
                        results.firstText()?.let { trySend(SpeechRecognitionEvent.Final(it)) }
                        close()
                    }

                    override fun onError(error: Int) {
                        trySend(SpeechRecognitionEvent.Failed(AndroidSpeechErrors.recognitionFailed(error)))
                        close()
                    }
                },
            )
            speechRecognizer.startListening(request.toIntent())
        }
        awaitClose {
            mainHandler.post {
                recognizer?.cancel()
                recognizer?.destroy()
                recognizer = null
            }
        }
    }

    private fun SpeechRecognitionRequest.toIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            locale?.let { putExtra(RecognizerIntent.EXTRA_LANGUAGE, it) }
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

    private fun Bundle?.firstText(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
}

object AndroidSpeechErrors {
    fun recognitionFailed(errorCode: Int): CapabilityError = CapabilityError(
        code = "speech_recognition.failed",
        message = "Android speech recognizer failed with code $errorCode.",
        recoverable = true,
    )
}
