package com.ai.phoneagent.platform.android.nativeruntime

import android.content.Context
import android.speech.SpeechRecognizer

interface SpeechRecognitionAvailability {
    fun isAvailable(): Boolean
}

class AndroidSpeechRecognitionAvailability(
    private val context: Context,
) : SpeechRecognitionAvailability {
    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)
}
