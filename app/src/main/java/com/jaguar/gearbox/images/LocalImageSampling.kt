package com.jaguar.gearbox.images

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import java.nio.ByteBuffer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.coroutines.coroutineContext

object ImageSamplingLimits {
    const val MAX_BYTES = 12 * 1024 * 1024
    const val MAX_SOURCE_EDGE = 12000
    const val MAX_SOURCE_PIXELS = 40_000_000L
    const val MAX_PREVIEW_EDGE = 1024
    fun validSource(width: Int, height: Int): Boolean = width > 0 && height > 0 &&
        width <= MAX_SOURCE_EDGE && height <= MAX_SOURCE_EDGE && width.toLong() * height <= MAX_SOURCE_PIXELS
    fun sampleSize(width: Int, height: Int): Int {
        require(validSource(width, height))
        var sample = 1
        while ((width + sample - 1) / sample > MAX_PREVIEW_EDGE ||
            (height + sample - 1) / sample > MAX_PREVIEW_EDGE) sample *= 2
        return sample
    }
}

/** No URI permissions are persisted and no image bytes are written to app storage. */
suspend fun loadLocalSamplingImage(resolver: ContentResolver, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
    require(uri.scheme == "content")
    val activeContext = coroutineContext
    val bytes = resolver.openInputStream(uri)?.use { readBoundedImageBytes(it) { activeContext.ensureActive() } }
        ?: throw IllegalArgumentException("Unavailable image")
    coroutineContext.ensureActive()
    val result = decodeSamplingImage(bytes)
    try { coroutineContext.ensureActive() } catch (cancelled: kotlinx.coroutines.CancellationException) {
        result.recycle(); throw cancelled
    }
    result
}

internal fun readBoundedImageBytes(input: InputStream, checkActive: () -> Unit = {}): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (true) {
        checkActive()
        val read = input.read(buffer)
        if (read < 0) break
        if (read == 0) continue
        total += read
        require(total <= ImageSamplingLimits.MAX_BYTES) { "Image too large" }
        output.write(buffer, 0, read)
    }
    require(total > 0) { "Empty image" }
    return output.toByteArray()
}

/** Decode bounds first, downsample, apply decoder orientation, then composite on white.
 * The returned sRGB bitmap is exactly the preview that is sampled, not a physical color reading. */
internal fun decodeSamplingImage(bytes: ByteArray): Bitmap {
    require(bytes.isNotEmpty() && bytes.size <= ImageSamplingLimits.MAX_BYTES)
    val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
        val width = info.size.width
        val height = info.size.height
        require(ImageSamplingLimits.validSource(width, height)) { "Unsupported dimensions" }
        val sample = ImageSamplingLimits.sampleSize(width, height)
        decoder.setTargetSize((width + sample - 1) / sample, (height + sample - 1) / sample)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
        decoder.setOnPartialImageListener { false }
    }
    try {
        require(decoded.width <= ImageSamplingLimits.MAX_PREVIEW_EDGE && decoded.height <= ImageSamplingLimits.MAX_PREVIEW_EDGE)
        val opaque = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888)
        Canvas(opaque).apply { drawColor(Color.WHITE); drawBitmap(decoded, 0f, 0f, null) }
        return opaque
    } finally { decoded.recycle() }
}

/** The image fills an aspect-matched box, so there are no letterbox offsets. */
fun displayedPixel(tapX: Float, tapY: Float, displayedWidth: Int, displayedHeight: Int,
    pixelWidth: Int, pixelHeight: Int): Pair<Int, Int>? {
    if (!tapX.isFinite() || !tapY.isFinite() || displayedWidth <= 0 || displayedHeight <= 0 ||
        pixelWidth <= 0 || pixelHeight <= 0 || tapX < 0 || tapY < 0 || tapX >= displayedWidth || tapY >= displayedHeight) return null
    return ((tapX / displayedWidth * pixelWidth).toInt().coerceIn(0, pixelWidth - 1) to
        (tapY / displayedHeight * pixelHeight).toInt().coerceIn(0, pixelHeight - 1))
}
