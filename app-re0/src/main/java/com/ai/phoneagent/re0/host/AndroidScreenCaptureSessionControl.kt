package com.ai.phoneagent.re0.host

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.ai.phoneagent.core.capability.CapabilityError
import com.ai.phoneagent.platform.android.screen.AndroidScreenCaptureRuntime
import com.ai.phoneagent.platform.android.screen.ScreenCaptureForegroundService
import com.ai.phoneagent.re0.generated.AutomationResultDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class AndroidScreenCaptureSessionControl(
    private val activity: ComponentActivity,
) : ScreenCaptureSessionControl {
    private var pendingCallback: ((Result<AutomationResultDto>) -> Unit)? = null
    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::handleConsentResult,
    )

    override fun requestConsent(callback: (Result<AutomationResultDto>) -> Unit) {
        if (AndroidScreenCaptureRuntime.session.health.value.available) {
            callback.success(screenCaptureReadyResult("Screen capture session is already ready."))
            return
        }
        if (pendingCallback != null) {
            callback.success(
                automationFailure(
                    code = "screen_capture.consent_in_progress",
                    message = "A screen capture consent request is already active.",
                    recoverable = true,
                ),
            )
            return
        }

        val manager = activity.getSystemService(MediaProjectionManager::class.java)
        pendingCallback = callback
        try {
            launcher.launch(manager.createScreenCaptureIntent())
        } catch (error: ActivityNotFoundException) {
            finishWithLaunchError(error::class.qualifiedName)
        } catch (error: SecurityException) {
            finishWithLaunchError(error::class.qualifiedName)
        } catch (error: IllegalStateException) {
            finishWithLaunchError(error::class.qualifiedName)
        }
    }

    override fun stopSession(): AutomationResultDto {
        ScreenCaptureForegroundService.stop(activity.applicationContext)
        return AutomationResultDto(
            success = true,
            summary = "Screen capture session stopped.",
            recoverable = false,
        )
    }

    fun close() {
        pendingCallback?.success(
            automationFailure(
                code = "screen_capture.consent_cancelled",
                message = "The host closed before screen capture consent completed.",
                recoverable = true,
            ),
        )
        pendingCallback = null
    }

    private fun handleConsentResult(result: ActivityResult) {
        val callback = pendingCallback ?: return
        val data = result.data
        if (result.resultCode != Activity.RESULT_OK || data == null) {
            pendingCallback = null
            callback.success(
                automationFailure(
                    code = "screen_capture.consent_denied",
                    message = "Screen capture consent was not granted.",
                    recoverable = true,
                ),
            )
            return
        }

        try {
            ScreenCaptureForegroundService.start(
                context = activity.applicationContext,
                resultCode = result.resultCode,
                resultData = Intent(data),
            )
        } catch (error: SecurityException) {
            finishWithStartError(error::class.qualifiedName)
            return
        } catch (error: IllegalStateException) {
            finishWithStartError(error::class.qualifiedName)
            return
        }

        activity.lifecycleScope.launch {
            val health = withTimeoutOrNull(SESSION_START_TIMEOUT_MS) {
                AndroidScreenCaptureRuntime.session.health.first { current ->
                    current.available || current.lastError?.code != "screen_capture.session_starting"
                }
            }
            pendingCallback = null
            if (health?.available == true) {
                callback.success(screenCaptureReadyResult("Screen capture session ready."))
            } else {
                callback.success(
                    automationFailure(
                        code = health?.lastError?.code ?: "screen_capture.session_start_timeout",
                        message = health?.lastError?.message
                            ?: "Android did not start screen capture within ${SESSION_START_TIMEOUT_MS}ms.",
                        recoverable = true,
                    ),
                )
            }
        }
    }

    private fun finishWithLaunchError(causeClass: String?) {
        val callback = pendingCallback ?: return
        pendingCallback = null
        callback.success(
            automationFailure(
                code = "screen_capture.consent_launch_failed",
                message = "Android could not open screen capture consent${causeClass.suffix()}.",
                recoverable = true,
            ),
        )
    }

    private fun finishWithStartError(causeClass: String?) {
        val callback = pendingCallback ?: return
        pendingCallback = null
        AndroidScreenCaptureRuntime.session.fail(
            CapabilityError(
                code = "screen_capture.service_start_failed",
                message = "Android could not start the screen capture service.",
                causeClass = causeClass,
                recoverable = true,
            ),
        )
        callback.success(
            automationFailure(
                code = "screen_capture.service_start_failed",
                message = "Android could not start the screen capture service${causeClass.suffix()}.",
                recoverable = true,
            ),
        )
    }

    private fun screenCaptureReadyResult(summary: String) = AutomationResultDto(
        success = true,
        summary = summary,
        recoverable = false,
        text = AndroidScreenCaptureRuntime.session.health.value.diagnostics.entries
            .joinToString(separator = "\n") { (key, value) -> "$key=$value" },
        mimeType = "text/plain",
    )

    private fun String?.suffix(): String = this?.let { " ($it)" }.orEmpty()

    private fun ((Result<AutomationResultDto>) -> Unit).success(value: AutomationResultDto) {
        this(Result.success(value))
    }

    private companion object {
        const val SESSION_START_TIMEOUT_MS = 10_000L
    }
}
