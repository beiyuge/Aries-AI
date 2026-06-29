package com.ai.phoneagent.platform.android.nativeruntime

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

interface MicrophonePermissionStatus {
    fun hasRecordAudioPermission(): Boolean
}

class AndroidMicrophonePermissionStatus(
    private val context: Context,
) : MicrophonePermissionStatus {
    override fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}
