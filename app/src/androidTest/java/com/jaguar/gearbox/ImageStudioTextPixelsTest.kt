package com.jaguar.gearbox
import android.graphics.Bitmap
import android.graphics.Color
import com.jaguar.gearbox.images.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
class ImageStudioTextPixelsTest {
    @Test fun textIsRenderedBeforeCropWithoutChangingSource() = runBlocking {
        val source=Bitmap.createBitmap(100,60,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.WHITE)}
        val result=transformStudioImage(source,ImageStudioCrop(10,10,100,60),90,50,
            listOf(ImageStudioEdit(ImageStudioEditKind.TEXT,ImageStudioCrop(10,10,100,60),strength=24,text="Hello")))
        try {
            val pixels=IntArray(90*50); result.getPixels(pixels,0,90,0,0,90,50)
            assertTrue(pixels.any { it != Color.WHITE })
            val original=IntArray(100*60);source.getPixels(original,0,100,0,0,100,60)
            assertTrue(original.all { it == Color.WHITE })
        } finally {source.recycle();result.recycle()}
    }
}
