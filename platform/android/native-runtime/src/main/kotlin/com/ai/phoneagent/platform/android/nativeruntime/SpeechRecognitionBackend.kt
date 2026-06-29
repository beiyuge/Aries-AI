package com.ai.phoneagent.platform.android.nativeruntime

import com.ai.phoneagent.core.capability.SpeechRecognitionEvent
import com.ai.phoneagent.core.capability.SpeechRecognitionRequest
import kotlinx.coroutines.flow.Flow

interface SpeechRecognitionBackend {
    val diagnostics: Map<String, String>
    fun recognize(request: SpeechRecognitionRequest): Flow<SpeechRecognitionEvent>
}
