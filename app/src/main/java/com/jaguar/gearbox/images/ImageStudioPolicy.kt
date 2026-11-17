package com.jaguar.gearbox.images

import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

object ImageStudioLimits {
    const val MAX_BYTES = 20 * 1024 * 1024
    const val MAX_SOURCE_EDGE = 12000
    const val MAX_SOURCE_PIXELS = 40_000_000L
    const val MAX_EDGE = 2048
    const val MAX_PIXELS = 4_000_000L
    fun validSource(width: Int, height: Int) = width > 0 && height > 0 &&
        width <= MAX_SOURCE_EDGE && height <= MAX_SOURCE_EDGE && width.toLong() * height <= MAX_SOURCE_PIXELS
    fun validOutput(width: Int, height: Int) = width > 0 && height > 0 &&
        width <= MAX_EDGE && height <= MAX_EDGE && width.toLong() * height <= MAX_PIXELS
    fun decodedSize(width: Int, height: Int): Pair<Int, Int> {
        require(validSource(width, height))
        val scale = min(1.0, min(MAX_EDGE.toDouble() / maxOf(width, height),
            sqrt(MAX_PIXELS.toDouble() / (width.toLong() * height))))
        return maxOf(1, floor(width * scale).toInt()) to maxOf(1, floor(height * scale).toInt())
    }
}

/** Coordinates in the oriented decoded bitmap. Right and bottom are exclusive. */
data class ImageStudioCrop(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun validFor(width: Int, height: Int) = left >= 0 && top >= 0 && right > left && bottom > top &&
        right <= width && bottom <= height
}

enum class ImageStudioFormat(val mimeType: String, val extension: String) {
    PNG("image/png", "png"), JPEG("image/jpeg", "jpg"), WEBP("image/webp", "webp")
}
