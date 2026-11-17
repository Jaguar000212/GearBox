package com.jaguar.gearbox
import android.graphics.Bitmap
import android.graphics.Color
import com.jaguar.gearbox.images.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test
class ImageStudioEditsTest {
    @Test fun editsAreOrderedBeforeCropAndSourceIsUntouched() = runBlocking {
        val source = Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val edits = listOf(ImageStudioEdit(ImageStudioEditKind.SOLID,ImageStudioCrop(2,2,6,6),Color.BLACK),
            ImageStudioEdit(ImageStudioEditKind.SOLID,ImageStudioCrop(3,3,5,5),Color.RED))
        val result = transformStudioImage(source,ImageStudioCrop(2,2,6,6),4,4,edits)
        try {
            assertEquals(4,result.width); assertEquals(4,result.height)
            assertEquals(Color.BLACK,result.getPixel(0,0)); assertEquals(Color.RED,result.getPixel(1,1))
            assertEquals(Color.WHITE,source.getPixel(3,3)); assertFalse(source.isRecycled)
        } finally { source.recycle(); result.recycle() }
    }
    @Test fun effectsStayInsideRegionAndKeepTransparentPixelsTransparent() = runBlocking {
        for (kind in listOf(ImageStudioEditKind.BLUR,ImageStudioEditKind.MOSAIC)) {
            val source = Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888)
            val result = transformStudioImage(source,ImageStudioCrop(0,0,8,8),8,8,
                listOf(ImageStudioEdit(kind,ImageStudioCrop(2,2,6,6),strength=2)))
            try { assertEquals(0,Color.alpha(result.getPixel(3,3))); assertEquals(0,result.getPixel(0,0)) }
            finally { source.recycle(); result.recycle() }
        }
    }
    @Test fun mosaicAveragesPixelsAndDoesNotChangeOutsideRegion() = runBlocking {
        val source = Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE); setPixel(1,1,Color.BLACK)
        }
        val result = transformStudioImage(source,ImageStudioCrop(0,0,4,4),4,4,
            listOf(ImageStudioEdit(ImageStudioEditKind.MOSAIC,ImageStudioCrop(1,1,3,3),strength=2)))
        try {
            assertEquals(191,Color.red(result.getPixel(1,1)))
            assertEquals(result.getPixel(1,1),result.getPixel(2,2))
            assertEquals(Color.WHITE,result.getPixel(0,0)); assertEquals(Color.BLACK,source.getPixel(1,1))
        } finally { source.recycle(); result.recycle() }
    }
    @Test fun cancellationStopsPixelEffectBeforePublishingIt() {
        val working = Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        var checks=0
        try {
            try {
                applyStudioEdits(working,listOf(ImageStudioEdit(ImageStudioEditKind.BLUR,ImageStudioCrop(0,0,8,8)))) {
                    if (++checks == 3) throw CancellationException("test")
                }
                fail("Expected cancellation")
            } catch (_: CancellationException) { assertEquals(Color.WHITE,working.getPixel(0,0)) }
        } finally { working.recycle() }
    }
}
