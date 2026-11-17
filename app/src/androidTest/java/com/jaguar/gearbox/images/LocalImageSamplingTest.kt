package com.jaguar.gearbox.images

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class LocalImageSamplingTest {
    @Test fun chosenPixelIsOpaqueWhiteCompositeAndNeighborsStayDistinct() {
        val source = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888)
        source.setPixel(0, 0, Color.argb(128, 255, 0, 0))
        source.setPixel(1, 0, Color.BLUE)
        val bytes = ByteArrayOutputStream().also { source.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        source.recycle()
        val preview = decodeSamplingImage(bytes)
        assertEquals(2, preview.width)
        assertEquals(Color.BLUE, preview.getPixel(1, 0))
        val composite = preview.getPixel(0, 0)
        assertEquals(255, Color.alpha(composite))
        assertEquals(255, Color.red(composite))
        assertTrue(Color.green(composite) in 126..128)
        assertTrue(Color.blue(composite) in 126..128)
        preview.recycle()
    }
    @Test fun oversizedStreamsAndInvalidImagesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            readBoundedImageBytes(ByteArray(ImageSamplingLimits.MAX_BYTES + 1).inputStream())
        }
        assertThrows(Exception::class.java) { decodeSamplingImage("not an image".toByteArray()) }
    }
    @Test fun boundedReaderHonorsCancellationBeforeReading() {
        assertThrows(kotlinx.coroutines.CancellationException::class.java) {
            readBoundedImageBytes(byteArrayOf(1).inputStream()) { throw kotlinx.coroutines.CancellationException() }
        }
    }
}
