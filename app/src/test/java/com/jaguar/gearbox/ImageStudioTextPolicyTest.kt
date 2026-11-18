package com.jaguar.gearbox
import com.jaguar.gearbox.images.*
import org.junit.Assert.*
import org.junit.Test
class ImageStudioTextPolicyTest {
    @Test fun unicodeBoundsAndMalformedText() {
        assertTrue(validStudioAnnotation("😀".repeat(120)))
        assertFalse(validStudioAnnotation("😀".repeat(121)))
        for (value in listOf("", "  ", "a\n", "a\r", "a\t", "a\u0000", "a\u2028", "a\u2029", "\uD800", "\uDC00"))
            assertFalse(value, validStudioAnnotation(value))
    }
    @Test fun textRequiresOpaqueColorFontBoundsAndImagePosition() {
        val edit = ImageStudioEdit(ImageStudioEditKind.TEXT, ImageStudioCrop(0,0,100,100), text="Hello", strength=16)
        assertTrue(edit.validFor(100,100))
        assertFalse(edit.copy(color=0x7f000000).validFor(100,100))
        assertFalse(edit.copy(strength=7).validFor(100,100))
        assertFalse(edit.copy(strength=129).validFor(100,100))
        assertFalse(edit.copy(region=ImageStudioCrop(100,0,100,100)).validFor(100,100))
        assertFalse(validStudioEdits(List(21){edit},100,100))
    }
}
