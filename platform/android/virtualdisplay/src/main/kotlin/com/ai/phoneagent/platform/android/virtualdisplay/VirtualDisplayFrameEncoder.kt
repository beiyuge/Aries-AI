package com.ai.phoneagent.platform.android.virtualdisplay

import android.graphics.Bitmap
import com.ai.phoneagent.core.capability.CaptureFormat
import com.ai.phoneagent.core.capability.CaptureRequest
import com.ai.phoneagent.core.capability.CaptureResult
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object VirtualDisplayFrameEncoder {
    suspend fun encode(
        frame: VirtualDisplayFrame,
        request: CaptureRequest,
        sourceName: String,
    ): CaptureResult = withContext(Dispatchers.Default) {
        val source = Bitmap.createBitmap(
            frame.pixels,
            frame.width,
            frame.height,
            Bitmap.Config.ARGB_8888,
        )
        val targetWidth = request.maxWidth
            ?.takeIf { it > 0 && it < frame.width }
            ?: frame.width
        val targetHeight = (frame.height.toLong() * targetWidth / frame.width)
            .toInt()
            .coerceAtLeast(1)
        val output = if (targetWidth == frame.width) {
            source
        } else {
            Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
        }

        try {
            val bytes = ByteArrayOutputStream().use { stream ->
                val format = when (request.format) {
                    CaptureFormat.Png -> Bitmap.CompressFormat.PNG
                    CaptureFormat.Jpeg -> Bitmap.CompressFormat.JPEG
                }
                val encoded = output.compress(format, JPEG_QUALITY, stream)
                if (!encoded) {
                    return@withContext CaptureResult(
                        source = sourceName,
                        error = VirtualDisplayErrors.operationFailed("Bitmap encoding failed."),
                    )
                }
                stream.toByteArray()
            }
            CaptureResult(
                bytes = bytes,
                width = targetWidth,
                height = targetHeight,
                source = sourceName,
            )
        } catch (error: IllegalArgumentException) {
            CaptureResult(
                source = sourceName,
                error = VirtualDisplayErrors.frameReadFailed(error::class.qualifiedName),
            )
        } catch (error: IllegalStateException) {
            CaptureResult(
                source = sourceName,
                error = VirtualDisplayErrors.frameReadFailed(error::class.qualifiedName),
            )
        } finally {
            if (output !== source) {
                output.recycle()
            }
            source.recycle()
        }
    }

    private const val JPEG_QUALITY = 92
}
