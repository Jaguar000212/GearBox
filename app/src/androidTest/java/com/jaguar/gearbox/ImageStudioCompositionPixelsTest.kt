package com.jaguar.gearbox
import android.graphics.Bitmap
import android.graphics.Color
import com.jaguar.gearbox.images.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class ImageStudioCompositionPixelsTest {
    @Test fun containAndCoverAreExplicitAndSourceUnchanged() = runBlocking {
        val a=Bitmap.createBitmap(4,2,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val b=Bitmap.createBitmap(4,2,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        try {
            val contain=collageStudioImages(listOf(a,b),8,8,StudioCollageLayout.HORIZONTAL,StudioImageFit.CONTAIN)
            val cover=collageStudioImages(listOf(a,b),8,8,StudioCollageLayout.HORIZONTAL,StudioImageFit.COVER)
            try {
                assertEquals(Color.WHITE,contain.getPixel(1,0)); assertEquals(Color.RED,contain.getPixel(1,4))
                assertEquals(Color.RED,cover.getPixel(1,0)); assertEquals(Color.BLUE,cover.getPixel(6,0))
                assertEquals(Color.RED,a.getPixel(0,0)); assertFalse(a.isRecycled)
            } finally { contain.recycle(); cover.recycle() }
        } finally { a.recycle(); b.recycle() }
    }
    @Test fun gridUnusedCellIsOpaqueBackgroundAndEnhancementRunsAfterComposition() = runBlocking {
        val image=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        try {
            val result=collageStudioImages(List(3){image},4,4,StudioCollageLayout.GRID,StudioImageFit.CONTAIN,
                enhancement=StudioEnhancement(saturation=-100))
            try {
                assertEquals(Color.WHITE,result.getPixel(3,3))
                val pixel=result.getPixel(0,0)
                assertEquals(Color.red(pixel),Color.blue(pixel)); assertEquals(255,Color.alpha(pixel))
                assertEquals(Color.RED,image.getPixel(0,0))
            } finally { result.recycle() }
        } finally { image.recycle() }
    }
    @Test fun cancellationAndOversizedSourcesFailWithoutChangingSources() = runBlocking {
        val bitmap=Bitmap.createBitmap(1001,1000,Bitmap.Config.ARGB_8888)
        try {
            try { collageStudioImages(listOf(bitmap,bitmap),4,4,StudioCollageLayout.GRID,StudioImageFit.CONTAIN); fail() }
            catch (_:IllegalArgumentException) { assertFalse(bitmap.isRecycled) }
            try { applyStudioEnhancement(bitmap,StudioEnhancement(brightness=10)) { throw CancellationException("test") }; fail() }
            catch (_:CancellationException) { assertEquals(0,bitmap.getPixel(0,0)) }
        } finally { bitmap.recycle() }
    }
}
