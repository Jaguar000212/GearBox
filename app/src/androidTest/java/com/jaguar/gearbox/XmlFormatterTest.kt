package com.jaguar.gearbox
import com.jaguar.gearbox.logic.*
import org.junit.Assert.*
import org.junit.Test
class XmlFormatterTest {
    @Test fun attributesFormatWithoutChangingMixedTextOrQuotedValues() {
        val source="<?xml version='1.0'?><root a = 'x  y' b=\"&amp;\">before <b>bold</b> after<![CDATA[<x>]]><!-- c --><?test value?></root>"
        val expected="<?xml version='1.0'?><root\n  a = 'x  y'\n  b=\"&amp;\">before <b>bold</b> after<![CDATA[<x>]]><!-- c --><?test value?></root>"
        assertEquals(expected,evaluateXml(source,JsonFormatMode.PRETTY).output)
        assertEquals(expected,evaluateXml(expected,JsonFormatMode.PRETTY).output)
        val preserve="<x xml:space='preserve'>  a\n<b/>  c </x>"
        assertEquals("<x\n  xml:space='preserve'>  a\n<b/>  c </x>",evaluateXml(preserve,JsonFormatMode.PRETTY).output)
    }
    @Test fun unsafeDeclarationsAndIllegalCharactersAreRejected() {
        for(source in listOf("<!DOCTYPE x SYSTEM 'https://example.com/entity'><x/>",
            "<!DOCTYPE x [<!ENTITY a SYSTEM 'file:///private'>]><x>&a;</x>",
            "<x>&unknown;</x>","<x>&#0;</x>","<x>&#xD800;</x>","<x>&#x110000;</x>",
            "<x a='&#0;'/>","<x>\u0000</x>","<x>\uD800</x>","<x a='1' a='2'/>","<p:x/>",
            "<?xml version='1.1'?><x/>","", "<x/><y/>")) {
            assertNotNull(source,evaluateXml(source,JsonFormatMode.VALIDATE).error)
        }
    }
    @Test fun declarationLookingTextIsConservativelyRejectedEvenWhenInert() {
        for(source in listOf(
            "<x><!-- <!DOCTYPE x> --></x>",
            "<x><![CDATA[<!ENTITY example 'value'>]]></x>",
            "<x><?example <!DOCTYPE x> ?></x>",
        )) {
            assertEquals(XmlFormatErrorKind.UNSAFE_DECLARATION,
                evaluateXml(source,JsonFormatMode.PRETTY).error?.kind)
        }
    }
    @Test fun namespacesAndBounds() {
        assertNull(evaluateXml("<x xmlns:p='urn:p' p:a='1'/>",JsonFormatMode.VALIDATE).error)
        assertNotNull(evaluateXml("<x xmlns:p='urn:p' xmlns:q='urn:p' p:a='1' q:a='2'/>",JsonFormatMode.VALIDATE).error)
        assertNotNull(evaluateXml("<x>".repeat(65)+"</x>".repeat(65),JsonFormatMode.VALIDATE).error)
        assertNotNull(evaluateXml("x".repeat(64001),JsonFormatMode.VALIDATE).error)
        assertNotNull(evaluateXml("<x>"+"<a/>".repeat(10001)+"</x>",JsonFormatMode.VALIDATE).error)
        assertNotNull(evaluateXml("<x/>",JsonFormatMode.MINIFY).error)
    }
}
