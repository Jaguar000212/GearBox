package com.jaguar.gearbox
import com.jaguar.gearbox.images.*
import org.junit.Assert.*
import org.junit.Test
class ImageStudioEditPolicyTest {
    private val region = ImageStudioCrop(0,0,10,10)
    @Test fun boundsAndOpaqueColorsAreEnforced() {
        ImageStudioEditKind.entries.filter { it != ImageStudioEditKind.ARROW }.forEach { assertTrue(ImageStudioEdit(it,region,text="A").validFor(10,10)) }
        assertFalse(ImageStudioEdit(ImageStudioEditKind.SOLID,region,color=0x00ffffff).validFor(10,10))
        assertFalse(ImageStudioEdit(ImageStudioEditKind.BLUR,region,strength=33).validFor(10,10))
        assertFalse(ImageStudioEdit(ImageStudioEditKind.MOSAIC,region,strength=1).validFor(10,10))
        assertFalse(ImageStudioEdit(ImageStudioEditKind.RECTANGLE,region,strength=0).validFor(10,10))
        assertFalse(ImageStudioEdit(ImageStudioEditKind.SOLID,ImageStudioCrop(1,1,1,2)).validFor(10,10))
        assertFalse(ImageStudioEdit(ImageStudioEditKind.SOLID,region).validFor(9,10))
        assertFalse(ImageStudioEdit(ImageStudioEditKind.ARROW,ImageStudioCrop(1,1,1,1)).validFor(10,10))
    }
    @Test fun atMostTwentyOperationsAndBoundedWorkingBitmap() {
        val operation = ImageStudioEdit(ImageStudioEditKind.SOLID,region)
        assertTrue(validStudioEdits(List(20){operation},10,10))
        assertFalse(validStudioEdits(List(21){operation},10,10))
        assertFalse(validStudioEdits(emptyList(),12000,12000))
    }
    @Test fun arrowsAcceptEveryDirectionButRejectOutOfBoundsOrIdenticalEndpoints() {
        listOf(ImageStudioCrop(0,0,9,9), ImageStudioCrop(9,9,0,0),
            ImageStudioCrop(0,9,9,0), ImageStudioCrop(9,0,0,9),
            ImageStudioCrop(5,9,5,0), ImageStudioCrop(9,5,0,5)).forEach {
            assertTrue(ImageStudioEdit(ImageStudioEditKind.ARROW,it).validFor(10,10))
        }
        assertFalse(ImageStudioEdit(ImageStudioEditKind.ARROW,ImageStudioCrop(0,0,10,9)).validFor(10,10))
        assertFalse(ImageStudioEdit(ImageStudioEditKind.ARROW,ImageStudioCrop(-1,0,9,9)).validFor(10,10))
    }

}
