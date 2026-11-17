package com.jaguar.gearbox.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ImageStudioTest {
    @Test fun decodePreservesAlphaAndCropDoesNotMutateSource() = runBlocking {
        val source = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888)
        source.setPixel(0, 0, Color.TRANSPARENT)
        source.setPixel(1, 0, Color.BLUE)
        val bytes = ByteArrayOutputStream().also { source.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        val decoded = decodeStudioImage(bytes)
        assertEquals(2, decoded.originalWidth)
        assertEquals(0, Color.alpha(decoded.bitmap.getPixel(0, 0)))
        val cropped = transformStudioImage(decoded.bitmap, ImageStudioCrop(1, 0, 2, 1), 1, 1)
        assertEquals(Color.BLUE, cropped.getPixel(0, 0))
        assertEquals(2, decoded.bitmap.width)
        cropped.recycle(); decoded.bitmap.recycle(); source.recycle()
    }
    @Test fun exportsStripContainerAndJpegCompositesWhiteAndFreshFilesRemain() = runBlocking {
        val root = File(InstrumentationRegistry.getInstrumentation().context.cacheDir, "studio-test-${UUID.randomUUID()}")
        root.mkdirs()
        val source = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        try {
            val png = exportStudioImage(root, source, ImageStudioFormat.PNG, 100)
            val jpg = exportStudioImage(root, source, ImageStudioFormat.JPEG, 100)
            val webp = exportStudioImage(root, source, ImageStudioFormat.WEBP, 90)
            val pngBitmap = BitmapFactory.decodeFile(png.file.path)
            val jpegBitmap = BitmapFactory.decodeFile(jpg.file.path)
            val webpBitmap = BitmapFactory.decodeFile(webp.file.path)
            assertEquals(0, Color.alpha(pngBitmap.getPixel(0, 0)))
            assertEquals(Color.WHITE, jpegBitmap.getPixel(0, 0))
            assertEquals(0, Color.alpha(webpBitmap.getPixel(0, 0)))
            pngBitmap.recycle(); jpegBitmap.recycle(); webpBitmap.recycle()
            repeat(9) { exportStudioImage(root, source, ImageStudioFormat.PNG, 100) }
            try { exportStudioImage(root, source, ImageStudioFormat.PNG, 100); fail("Expected full cache") }
            catch (_: IllegalStateException) { }
            assertTrue(png.file.exists())
            assertEquals(12, File(root, "image_studio").listFiles()!!.size)
        } finally { source.recycle(); root.deleteRecursively() }
    }
    @Test fun encodedLimitAndCancellationRejectBeforeDecode() {
        assertThrows(IllegalArgumentException::class.java) {
            readStudioBytes(object : java.io.InputStream() {
                override fun read(): Int = 0
                override fun read(bytes: ByteArray, offset: Int, length: Int): Int = 0
            })
        }
        assertThrows(IllegalArgumentException::class.java) { readStudioBytes(ByteArray(ImageStudioLimits.MAX_BYTES + 1).inputStream()) }
        assertThrows(kotlinx.coroutines.CancellationException::class.java) {
            readStudioBytes(byteArrayOf(1).inputStream()) { throw kotlinx.coroutines.CancellationException() }
        }
        assertThrows(Exception::class.java) { decodeStudioImage(byteArrayOf(1, 2, 3)) }
    }
}
