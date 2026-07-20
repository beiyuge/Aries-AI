package com.ai.phoneagent.platform.android.virtualdisplay

import android.graphics.Bitmap
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import com.ai.phoneagent.core.capability.CapabilityError
import java.nio.BufferUnderflowException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class VirtualDisplayFrameBuffer {
    private val _frames = MutableStateFlow<VirtualDisplayFrame?>(null)
    private val _lastError = MutableStateFlow<CapabilityError?>(null)

    val frames = _frames.asStateFlow()
    val lastError = _lastError.asStateFlow()

    fun attach(reader: ImageReader, handler: Handler) {
        reader.setOnImageAvailableListener(::onImageAvailable, handler)
    }

    fun clear() {
        _frames.value = null
        _lastError.value = null
    }

    private fun onImageAvailable(reader: ImageReader) {
        val image = try {
            reader.acquireLatestImage()
        } catch (error: IllegalStateException) {
            _lastError.value = VirtualDisplayErrors.frameReadFailed(error::class.qualifiedName)
            null
        } ?: return

        try {
            image.toFrame()?.let { frame ->
                _frames.value = frame
                _lastError.value = null
            }
        } catch (error: IllegalArgumentException) {
            _lastError.value = VirtualDisplayErrors.frameReadFailed(error::class.qualifiedName)
        } catch (error: IllegalStateException) {
            _lastError.value = VirtualDisplayErrors.frameReadFailed(error::class.qualifiedName)
        } catch (error: BufferUnderflowException) {
            _lastError.value = VirtualDisplayErrors.frameReadFailed(error::class.qualifiedName)
        } finally {
            image.close()
        }
    }

    private fun Image.toFrame(): VirtualDisplayFrame? {
        val plane = planes.firstOrNull() ?: return null
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        if (pixelStride <= 0 || rowStride < width * pixelStride) {
            return null
        }

        val paddedWidth = rowStride / pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
        try {
            plane.buffer.rewind()
            padded.copyPixelsFromBuffer(plane.buffer)
            val visible = if (paddedWidth == width) {
                padded
            } else {
                Bitmap.createBitmap(padded, 0, 0, width, height)
            }
            try {
                val pixels = IntArray(width * height)
                visible.getPixels(pixels, 0, width, 0, 0, width, height)
                return VirtualDisplayFrame(
                    width = width,
                    height = height,
                    pixels = pixels,
                    capturedAtMs = System.currentTimeMillis(),
                )
            } finally {
                if (visible !== padded) {
                    visible.recycle()
                }
            }
        } finally {
            padded.recycle()
        }
    }
}
