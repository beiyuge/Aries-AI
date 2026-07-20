package com.ai.phoneagent.platform.android.virtualdisplay

internal data class VirtualDisplayFrame(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
    val capturedAtMs: Long,
)

internal data class VirtualDisplayFrameAnalysis(
    val sampledPixels: Int,
    val averageLuma: Double,
    val darkPixelRatio: Double,
    val likelyBlack: Boolean,
)

internal object VirtualDisplayFrameAnalyzer {
    fun analyze(frame: VirtualDisplayFrame): VirtualDisplayFrameAnalysis {
        if (frame.pixels.isEmpty()) {
            return VirtualDisplayFrameAnalysis(
                sampledPixels = 0,
                averageLuma = 0.0,
                darkPixelRatio = 1.0,
                likelyBlack = true,
            )
        }

        val stride = (frame.pixels.size / MAX_SAMPLES).coerceAtLeast(1)
        var sampled = 0
        var dark = 0
        var lumaTotal = 0.0
        var index = 0
        while (index < frame.pixels.size) {
            val pixel = frame.pixels[index]
            val red = pixel shr 16 and 0xFF
            val green = pixel shr 8 and 0xFF
            val blue = pixel and 0xFF
            val luma = RED_WEIGHT * red + GREEN_WEIGHT * green + BLUE_WEIGHT * blue
            lumaTotal += luma
            if (red <= DARK_CHANNEL_THRESHOLD &&
                green <= DARK_CHANNEL_THRESHOLD &&
                blue <= DARK_CHANNEL_THRESHOLD
            ) {
                dark += 1
            }
            sampled += 1
            index += stride
        }

        val averageLuma = lumaTotal / sampled
        val darkPixelRatio = dark.toDouble() / sampled
        return VirtualDisplayFrameAnalysis(
            sampledPixels = sampled,
            averageLuma = averageLuma,
            darkPixelRatio = darkPixelRatio,
            likelyBlack = averageLuma <= BLACK_AVERAGE_LUMA_THRESHOLD &&
                darkPixelRatio >= BLACK_PIXEL_RATIO_THRESHOLD,
        )
    }

    private const val MAX_SAMPLES = 4_096
    private const val DARK_CHANNEL_THRESHOLD = 8
    private const val BLACK_AVERAGE_LUMA_THRESHOLD = 4.0
    private const val BLACK_PIXEL_RATIO_THRESHOLD = 0.995
    private const val RED_WEIGHT = 0.2126
    private const val GREEN_WEIGHT = 0.7152
    private const val BLUE_WEIGHT = 0.0722
}
