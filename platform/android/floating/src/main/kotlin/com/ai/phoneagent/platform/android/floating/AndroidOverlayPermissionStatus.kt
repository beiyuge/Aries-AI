package com.ai.phoneagent.platform.android.floating

import android.content.Context
import android.provider.Settings

class AndroidOverlayPermissionStatus(
    private val context: Context,
) : OverlayPermissionStatus {
    override fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)
}
