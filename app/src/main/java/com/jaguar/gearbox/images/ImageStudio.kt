package com.jaguar.gearbox.images

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

internal val studioWorkMutex = Mutex()

/** Caller owns bitmap lifecycle. Original dimensions are decoder-reported source bounds; bitmap dimensions are authoritative for edits. */
data class ImageStudioImage(val bitmap: Bitmap, val originalWidth: Int, val originalHeight: Int)
data class ImageStudioExport(val file: File, val mimeType: String)

/** Only the URI explicitly selected by the user; never persists URI permission or copies originals to disk. */
suspend fun loadStudioImage(resolver: ContentResolver, uri: Uri, maxWorkingPixels: Long = ImageStudioLimits.MAX_PIXELS): ImageStudioImage {
    var pending: Bitmap? = null
    try { return withContext(Dispatchers.IO) { studioWorkMutex.withLock {
    require(uri.scheme == "content")
    require(maxWorkingPixels in 1..ImageStudioLimits.MAX_PIXELS)
    val active = coroutineContext
    val bytes = resolver.openInputStream(uri)?.use { readStudioBytes(it) { active.ensureActive() } }
        ?: throw IllegalArgumentException("Unavailable image")
    active.ensureActive()
    val result = decodeStudioImage(bytes, maxWorkingPixels)
    pending = result.bitmap
    try { active.ensureActive(); result } catch (error: Throwable) { result.bitmap.recycle(); throw error }
}
    } } catch (error: Throwable) { pending?.let { if (!it.isRecycled) it.recycle() }; throw error }
}

internal fun readStudioBytes(input: InputStream, checkActive: () -> Unit = {}): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    var emptyReads = 0
    while (true) {
        checkActive()
        val count = input.read(buffer)
        if (count < 0) break
        if (count == 0) {
            require(++emptyReads <= 16) { "Image provider returned no data" }
            continue
        }
        emptyReads = 0
        total += count
        require(total <= ImageStudioLimits.MAX_BYTES) { "Image exceeds encoded size limit" }
        output.write(buffer, 0, count)
    }
    require(total > 0) { "Empty image" }
    return output.toByteArray()
}

internal fun decodeStudioImage(bytes: ByteArray, maxWorkingPixels: Long = ImageStudioLimits.MAX_PIXELS): ImageStudioImage {
    require(bytes.isNotEmpty() && bytes.size <= ImageStudioLimits.MAX_BYTES)
    var originalWidth = 0
    var originalHeight = 0
    val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, info, _ ->
        originalWidth = info.size.width
        originalHeight = info.size.height
        val size = studioWorkingSize(originalWidth, originalHeight, maxWorkingPixels)
        decoder.setTargetSize(size.first, size.second)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
        decoder.setOnPartialImageListener { false }
    }
    if (!ImageStudioLimits.validOutput(bitmap.width, bitmap.height)) {
        bitmap.recycle()
        throw IllegalArgumentException("Unsupported decoded dimensions")
    }
    return ImageStudioImage(bitmap, originalWidth, originalHeight)
}

/** Never mutates or recycles source. Caller must keep source alive until this returns. */
suspend fun transformStudioImage(source: Bitmap, crop: ImageStudioCrop, outputWidth: Int, outputHeight: Int,
    edits: List<ImageStudioEdit> = emptyList(),
    enhancement: StudioEnhancement = StudioEnhancement()): Bitmap {
    var pending: Bitmap? = null
    try { return withContext(Dispatchers.Default) { studioWorkMutex.withLock {
        require(!source.isRecycled && ImageStudioLimits.validOutput(source.width, source.height))
        require(crop.validFor(source.width, source.height))
        val operations = edits.toList()
        require(validStudioEdits(operations, source.width, source.height))
        require(enhancement.valid())
        require(ImageStudioLimits.validOutput(outputWidth, outputHeight))
        coroutineContext.ensureActive()
        val result = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        pending = result
        var working: Bitmap? = null
        try {
            val active = coroutineContext
            val edited = if (operations.isEmpty()) source else {
                requireNotNull(source.copy(Bitmap.Config.ARGB_8888, true)).also { copy ->
                    working = copy
                    applyStudioEdits(copy, operations) { active.ensureActive() }
                }
            }
            Canvas(result).drawBitmap(edited, Rect(crop.left, crop.top, crop.right, crop.bottom),
                Rect(0, 0, outputWidth, outputHeight), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            applyStudioEnhancement(result, enhancement) { active.ensureActive() }
            coroutineContext.ensureActive()
            result
        } catch (error: Throwable) { result.recycle(); throw error }
        finally { working?.recycle() }
    }
    } } catch (error: Throwable) { pending?.let { if (!it.isRecycled) it.recycle() }; throw error }
}

private val exportName = Regex("studio-[0-9]+\\.(png|jpg|webp)")
/** Re-encodes pixels only, without copying source metadata. Content itself is not anonymized.
 * Keeps at most 12 exports / 128 MiB; only exports older than 24 hours may be pruned.
 * Fresh exports are preserved for share recipients; a full cache rejects new exports.
 * Does not write, delete or replace the selected original. JPEG alpha is composited on white.
 */
suspend fun exportStudioImage(cacheDir: File, bitmap: Bitmap, format: ImageStudioFormat, quality: Int): ImageStudioExport {
    var pending: File? = null
    try { return withContext(Dispatchers.IO) { studioWorkMutex.withLock {
        require(!bitmap.isRecycled && ImageStudioLimits.validOutput(bitmap.width, bitmap.height))
        require(quality in 1..100)
        val active = coroutineContext
        active.ensureActive()
        run {
            val directory = File(cacheDir, "image_studio")
            check(directory.isDirectory || directory.mkdirs()) { "Cannot create export cache" }
            val old = directory.listFiles().orEmpty().filter { it.isFile && exportName.matches(it.name) }
                .sortedBy { it.lastModified() }
            val cutoff = System.currentTimeMillis() - 24L * 60 * 60 * 1000
            old.filter { it.lastModified() < cutoff }.forEach { check(it.delete()) { "Cannot prune export cache" } }
            val retained = directory.listFiles().orEmpty().filter { it.isFile && exportName.matches(it.name) }
            check(retained.size < 12 && retained.sumOf { it.length() } + ImageStudioLimits.MAX_BYTES <= 128L * 1024 * 1024) {
                "Export cache full; retry after older temporary exports expire"
            }
            val file = File.createTempFile("studio-", ".${format.extension}", directory)
            pending = file
            var opaque: Bitmap? = null
            try {
                val image = if (format == ImageStudioFormat.JPEG) {
                    Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).also {
                        opaque = it
                        Canvas(it).apply { drawColor(Color.WHITE); drawBitmap(bitmap, 0f, 0f, null) }
                    }
                } else bitmap
                val compression = when (format) {
                    ImageStudioFormat.PNG -> Bitmap.CompressFormat.PNG
                    ImageStudioFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    ImageStudioFormat.WEBP -> Bitmap.CompressFormat.WEBP_LOSSY
                }
                file.outputStream().use { raw ->
                    val bounded = object : OutputStream() {
                        var written = 0L
                        override fun write(value: Int) { write(byteArrayOf(value.toByte()), 0, 1) }
                        override fun write(bytes: ByteArray, offset: Int, length: Int) {
                            active.ensureActive()
                            check(written + length <= ImageStudioLimits.MAX_BYTES) { "Export exceeds size limit" }
                            raw.write(bytes, offset, length)
                            written += length
                        }
                    }
                    check(image.compress(compression, quality, bounded)) { "Cannot encode image" }
                }
                active.ensureActive()
                check(file.length() > 0)
                ImageStudioExport(file, format.mimeType)
            } catch (error: Throwable) { file.delete(); throw error }
            finally { opaque?.recycle() }
        }
    }

    } } catch (error: Throwable) { pending?.delete(); throw error }
}
