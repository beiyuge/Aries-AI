package com.ai.phoneagent.platform.android.screen

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.ai.phoneagent.core.capability.CapabilityError

class ScreenCaptureForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
        )
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val resultData = intent?.screenCaptureResultData()
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            AndroidScreenCaptureRuntime.session.fail(
                CapabilityError(
                    code = "screen_capture.invalid_consent_result",
                    message = "Android returned an invalid MediaProjection consent result.",
                    recoverable = true,
                ),
            )
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val manager = getSystemService(MediaProjectionManager::class.java)
        try {
            val projection = manager.getMediaProjection(resultCode, resultData)
            if (projection == null) {
                AndroidScreenCaptureRuntime.session.fail(
                    CapabilityError(
                        code = "screen_capture.session_missing",
                        message = "Android returned no MediaProjection session.",
                        recoverable = true,
                        suggestedAction = "Request screen capture consent again.",
                    ),
                )
                stopSelf(startId)
            } else {
                AndroidScreenCaptureRuntime.session.attach(applicationContext, projection)
            }
        } catch (error: SecurityException) {
            AndroidScreenCaptureRuntime.session.fail(
                sessionStartError(error::class.qualifiedName),
            )
            stopSelf(startId)
        } catch (error: IllegalStateException) {
            AndroidScreenCaptureRuntime.session.fail(
                sessionStartError(error::class.qualifiedName),
            )
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        AndroidScreenCaptureRuntime.session.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Screen capture",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Keeps the active Aries AI screen capture session visible."
            },
        )
    }

    private fun buildNotification(): Notification = Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_camera)
        .setContentTitle("Aries AI screen capture")
        .setContentText("Screen capture is active")
        .setOngoing(true)
        .setCategory(Notification.CATEGORY_SERVICE)
        .build()

    private fun sessionStartError(causeClass: String?): CapabilityError = CapabilityError(
        code = "screen_capture.session_start_failed",
        message = "Android could not start the MediaProjection session.",
        recoverable = true,
        causeClass = causeClass,
        suggestedAction = "Request screen capture consent again.",
    )

    private fun Intent.screenCaptureResultData(): Intent? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(EXTRA_RESULT_DATA)
    }

    companion object {
        private const val CHANNEL_ID = "aries_re0_screen_capture"
        private const val NOTIFICATION_ID = 4102
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"

        fun start(context: Context, resultCode: Int, resultData: Intent) {
            AndroidScreenCaptureRuntime.session.markStarting()
            ContextCompat.startForegroundService(
                context,
                Intent(context, ScreenCaptureForegroundService::class.java).apply {
                    putExtra(EXTRA_RESULT_CODE, resultCode)
                    putExtra(EXTRA_RESULT_DATA, resultData)
                },
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenCaptureForegroundService::class.java))
            AndroidScreenCaptureRuntime.session.stop()
        }
    }
}
