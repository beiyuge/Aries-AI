package com.ai.phoneagent.platform.android.virtualdisplay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualDisplayFrameAnalyzerTest {
    @Test
    fun `detects a near-black frame`() {
        val frame = frameOf(IntArray(10_000) { 0xFF000000.toInt() })

        val analysis = VirtualDisplayFrameAnalyzer.analyze(frame)

        assertTrue(analysis.likelyBlack)
        assertTrue(analysis.darkPixelRatio >= 0.995)
    }

    @Test
    fun `keeps a frame with visible content`() {
        val pixels = IntArray(10_000) { 0xFF000000.toInt() }
        for (index in 0 until 1_000) {
            pixels[index] = 0xFFFFFFFF.toInt()
        }

        val analysis = VirtualDisplayFrameAnalyzer.analyze(frameOf(pixels))

        assertFalse(analysis.likelyBlack)
    }

    private fun frameOf(pixels: IntArray) = VirtualDisplayFrame(
        width = 100,
        height = 100,
        pixels = pixels,
        capturedAtMs = 0,
    )
}
