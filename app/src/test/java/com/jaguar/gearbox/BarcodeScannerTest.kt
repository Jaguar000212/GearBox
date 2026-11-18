package com.jaguar.gearbox
import com.jaguar.gearbox.camera.*
import com.jaguar.gearbox.data.ToolMemoryPolicy
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test
class BarcodeScannerTest {
    @Test fun luminanceHonorsPositionLimitAndStrides(){
        val buffer=ByteBuffer.wrap(byteArrayOf(99,1,9,2,9,9,3,9,4,9));buffer.position(1);buffer.limit(9)
        assertArrayEquals(byteArrayOf(1,2,3,4),scannerLuminance(buffer,2,2,5,2))
        buffer.limit(8);assertNull(scannerLuminance(buffer,2,2,5,2))
        assertNull(scannerLuminance(buffer,2,2,1,2))
        assertNull(scannerLuminance(buffer,2000,2000,2000,1))
        assertEquals(1,buffer.position())
    }
    @Test fun acceptsOnlyExplicitSafeHttpDestinations(){
        for(url in listOf("https://example.com/a?q=1", "http://example.com:8080"))assertEquals(url,safeScannerHttpUrl(url))
        for(url in listOf("javascript:alert(1)","intent://x","https://user:pass@example.com","https://example.com\\@evil.com","https://example.com/%0a","https://example.com/%20","https://example.com:99999","https://example.com:","https://example.com:0","https://例子.com","https://example.com/\u200b","https://example.com/ a","https://%65xample.com"))assertNull(url,safeScannerHttpUrl(url))
        assertFalse(ToolMemoryPolicy.permitsRoute("tool/barcode_scanner"))
    }
    @Test fun qrDecodesLocallyWithoutChangingInputBytes(){
        val matrix=QRCodeWriter().encode("test payload",BarcodeFormat.QR_CODE,160,160)
        val bytes=ByteArray(160*160){if(matrix[it%160,it/160])0 else 255.toByte()};val copy=bytes.copyOf()
        assertEquals("test payload",decodeScannerFrame(bytes,160,160)?.text)
        assertArrayEquals(copy,bytes)
    }
}
