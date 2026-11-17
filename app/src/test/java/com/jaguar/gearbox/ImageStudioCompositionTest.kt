package com.jaguar.gearbox
import com.jaguar.gearbox.images.*
import org.junit.Assert.*
import org.junit.Test
class ImageStudioCompositionTest {
    @Test fun partitionHandlesOddSizesAndThreeImageBackgroundCell() {
        assertEquals(listOf(ImageStudioCrop(0,0,3,5),ImageStudioCrop(3,0,7,5)),studioCollageCells(2,7,5,StudioCollageLayout.HORIZONTAL))
        assertEquals(listOf(ImageStudioCrop(0,0,3,2),ImageStudioCrop(3,0,7,2),ImageStudioCrop(0,2,3,5)),studioCollageCells(3,7,5,StudioCollageLayout.GRID))
        assertEquals(4,studioCollageCells(4,8,8,StudioCollageLayout.VERTICAL).size)
    }
    @Test fun dedicatedDecodeBudgetAndLimits() {
        val (w,h)=studioWorkingSize(8000,4000,STUDIO_COLLAGE_SOURCE_PIXELS)
        assertTrue(w.toLong()*h<=STUDIO_COLLAGE_SOURCE_PIXELS)
        assertTrue(w<=2048 && h<=2048)
        assertEquals(100 to 50,studioWorkingSize(100,50,STUDIO_COLLAGE_SOURCE_PIXELS))
        listOf(1,5).forEach { count ->
            try { studioCollageCells(count,8,8,StudioCollageLayout.GRID); fail() } catch (_:IllegalArgumentException) {}
        }
        try { studioCollageCells(4,1,1,StudioCollageLayout.GRID); fail() } catch (_:IllegalArgumentException) {}
    }
    @Test fun enhancementIsIdentityByDefaultAndPreservesAlpha() {
        val color=0x804080c0.toInt()
        assertEquals(color,enhanceStudioPixel(color,StudioEnhancement()))
        val gray=enhanceStudioPixel(color,StudioEnhancement(saturation=-100))
        assertEquals(128,gray ushr 24)
        assertEquals((gray ushr 16) and 255,gray and 255)
        assertEquals((gray ushr 8) and 255,gray and 255)
        assertEquals(0xffffffff.toInt(),enhanceStudioPixel(0xff204060.toInt(),StudioEnhancement(brightness=100)))
        assertEquals(0xff000000.toInt(),enhanceStudioPixel(0xff204060.toInt(),StudioEnhancement(brightness=-100)))
        assertEquals(0,enhanceStudioPixel(0x00123456,StudioEnhancement(contrast=1)))
        assertFalse(StudioEnhancement(contrast=101).valid())
    }
}
