package com.jaguar.gearbox.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
