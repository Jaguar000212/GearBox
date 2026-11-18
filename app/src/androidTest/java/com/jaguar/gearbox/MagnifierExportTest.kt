package com.jaguar.gearbox
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.jaguar.gearbox.camera.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
class MagnifierExportTest {
    @Test fun frameDimensionsRejectUnsafeAllocationBounds(){
        assertTrue(validMagnifierDimensions(1000,1000))
        assertTrue(validMagnifierDimensions(2000,1000))
        assertFalse(validMagnifierDimensions(2001,1000))
        assertFalse(validMagnifierDimensions(4097,1))
        assertFalse(validMagnifierDimensions(0,1000))
        assertFalse(validMagnifierDimensions(Int.MAX_VALUE,Int.MAX_VALUE))
    }
    @Test fun selectedFrameIsOneShotAndPngPreservesPixels(){
        val frame=Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888).apply{eraseColor(Color.RED)}
        val selection=MagnifierExportSelection()
        try {
            assertTrue(selection.select(frame));assertSame(frame,selection.take());assertNull(selection.take())
            assertTrue(selection.select(frame));selection.clear();assertNull(selection.take())
            val bytes=ByteArrayOutputStream();assertTrue(writeMagnifierPng(frame,bytes))
            val data=bytes.toByteArray();val decoded=BitmapFactory.decodeByteArray(data,0,data.size)
            try{assertEquals(Color.RED,decoded.getPixel(0,0))}finally{decoded.recycle()}
        }finally{frame.recycle()}
        assertFalse(selection.select(frame))
    }
}
