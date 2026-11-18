package com.jaguar.gearbox

import android.graphics.Bitmap
import android.graphics.Color
import com.jaguar.gearbox.images.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ImageStudioArrowTest {
    @Test fun reverseAxisAndDiagonalArrowsRenderWithoutMutatingSource() = runBlocking {
        val endpoints = listOf(ImageStudioCrop(2,8,13,8), ImageStudioCrop(13,8,2,8),
            ImageStudioCrop(8,2,8,13), ImageStudioCrop(8,13,8,2),
            ImageStudioCrop(2,2,13,13), ImageStudioCrop(13,13,2,2),
            ImageStudioCrop(2,13,13,2), ImageStudioCrop(13,2,2,13))
        for (coordinates in endpoints) {
            val source = Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
            val result = transformStudioImage(source,ImageStudioCrop(0,0,16,16),16,16,
                listOf(ImageStudioEdit(ImageStudioEditKind.ARROW,coordinates,Color.BLACK,2)))
            try {
                assertEquals(16,result.width); assertEquals(16,result.height)
                val pixels = IntArray(256)
                result.getPixels(pixels,0,16,0,0,16,16)
                assertTrue(pixels.any { it == Color.BLACK })
                assertTrue(pixels.all { Color.alpha(it) == 255 })
                source.getPixels(pixels,0,16,0,0,16,16)
                assertTrue(pixels.all { it == Color.WHITE })
            } finally { result.recycle(); source.recycle() }
        }
    }

    @Test fun cancelledArrowWorkDoesNotStartDrawing() {
        val bitmap = Bitmap.createBitmap(16,16,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        try {
            try {
                applyStudioEdits(bitmap,listOf(ImageStudioEdit(ImageStudioEditKind.ARROW,
                    ImageStudioCrop(13,13,2,2)))) { throw CancellationException("test") }
                fail("Expected cancellation")
            } catch (_: CancellationException) {
                val pixels = IntArray(256)
                bitmap.getPixels(pixels,0,16,0,0,16,16)
                assertTrue(pixels.all { it == Color.WHITE })
            }
        } finally { bitmap.recycle() }
    }
}
