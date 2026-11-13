package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

class QrCodeTest {

    @Test
    fun `encodes non-empty text to a square matrix of the requested size`() {
        val matrix = generateQrMatrix("https://example.com", size = 256)
        assertNotNull(matrix)
        assertEquals(256, matrix!!.width)
        assertEquals(256, matrix.height)
    }

    @Test
    fun `matrix has at least some dark modules for real content`() {
        val matrix = generateQrMatrix("hello world")!!
        var darkCount = 0
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                if (matrix.get(x, y)) darkCount++
            }
        }
        assertTrue(darkCount > 0)
    }

    @Test
    fun `empty text returns null instead of encoding`() {
        assertNull(generateQrMatrix(""))
    }
    @Test
    fun `unicode and whitespace round trip without substitution`() {
        val text = "  नमस्ते 世界 👋  "
        val matrix = generateQrMatrix(text)!!
        val pixels = IntArray(matrix.width * matrix.height) { index ->
            if (matrix[index % matrix.width, index / matrix.width]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(matrix.width, matrix.height, pixels)))
        assertEquals(text, MultiFormatReader().decode(bitmap).text)
    }

    @Test
    fun `invalid size and excess payload fail gracefully`() {
        assertNull(generateQrMatrix("hello", size = 0))
        assertNull(generateQrMatrix("hello", size = -1))
        assertNull(generateQrMatrix("x".repeat(5000)))
    }
}
