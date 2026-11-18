package com.jaguar.gearbox.camera

import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.net.URI
import java.nio.ByteBuffer

enum class BarcodeScannerStatus { STOPPED, STARTING, SCANNING, RESULT, UNAVAILABLE, PERMISSION_REQUIRED, ERROR, TOO_LARGE }
data class ScannedBarcode(val text:String,val format:String)
data class BarcodeScannerState(val status:BarcodeScannerStatus=BarcodeScannerStatus.STOPPED,val result:ScannedBarcode?=null)
fun validScannerDimensions(width:Int,height:Int)=width>0 && height>0 && width<=1280 && height<=1280 && width.toLong()*height<=1_000_000
/** Reads only bounded luminance, respecting the plane buffer's current position and limit. */
fun scannerLuminance(buffer:ByteBuffer,width:Int,height:Int,rowStride:Int,pixelStride:Int):ByteArray? {
    if(!validScannerDimensions(width,height) || rowStride<=0 || pixelStride<=0)return null
    val rowBytes=(width-1L)*pixelStride+1
    if(rowBytes>rowStride)return null
    val start=buffer.position();val end=start.toLong()+(height-1L)*rowStride+rowBytes
    if(end>buffer.limit() || end<Int.MIN_VALUE)return null
    return ByteArray(width*height).also{out->
        for(y in 0 until height)for(x in 0 until width)out[y*width+x]=buffer.get(start+y*rowStride+x*pixelStride)
    }
}
private val scannerFormats=listOf(BarcodeFormat.QR_CODE,BarcodeFormat.DATA_MATRIX,BarcodeFormat.AZTEC,BarcodeFormat.PDF_417,BarcodeFormat.CODE_128,BarcodeFormat.CODE_39,BarcodeFormat.EAN_13,BarcodeFormat.EAN_8,BarcodeFormat.UPC_A,BarcodeFormat.UPC_E,BarcodeFormat.ITF)
/** No I/O. The caller owns and wipes luminance bytes. One bounded quarter-turn retry. */
fun decodeScannerFrame(bytes:ByteArray,width:Int,height:Int):ScannedBarcode? {
    if(!validScannerDimensions(width,height) || bytes.size!=width*height)return null
    val reader=MultiFormatReader().apply{setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to scannerFormats))}
    var rotated:ByteArray?=null
    try {
        repeat(2){attempt->
            val data=if(attempt==0)bytes else ByteArray(bytes.size).also{out->
                rotated=out
                for(y in 0 until height)for(x in 0 until width)out[(width-1-x)*height+y]=bytes[y*width+x]
            }
            val w=if(attempt==0)width else height;val h=if(attempt==0)height else width
            val source=PlanarYUVLuminanceSource(data,w,h,0,0,w,h,false)
            try{val result=reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)));return ScannedBarcode(result.text,result.barcodeFormat.name)}
            catch(_:ReaderException){}finally{reader.reset()}
        }
        return null
    }finally{rotated?.fill(0)}
}

fun safeScannerHttpUrl(text:String):String? {
    if(text.isEmpty() || text.length>4096 || text.any{it=='\\' || it.isWhitespace() || Character.isISOControl(it) || Character.getType(it)==Character.FORMAT.toInt()})return null
    if(Regex("%(?:0[0-9a-f]|1[0-9a-f]|20|7f)",RegexOption.IGNORE_CASE).containsMatchIn(text))return null
    val uri=try{URI(text)}catch(_:Exception){return null}
    if(uri.scheme?.lowercase() !in setOf("http","https") || uri.isOpaque || uri.rawUserInfo!=null)return null
    val authority=uri.rawAuthority?:return null
    val host=uri.host?:return null
    if(host.isEmpty() || host.any{it.code>127} || '%' in authority || '@' in authority || uri.port !in -1..65535 || uri.port==0)return null
    if(authority.endsWith(":"))return null
    return text
}
fun scannerUrlHost(text:String):String?=safeScannerHttpUrl(text)?.let{URI(it).host}
