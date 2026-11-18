package com.jaguar.gearbox.camera
import android.graphics.Bitmap
import java.io.OutputStream

enum class MagnifierStatus { STOPPED, STARTING, LIVE, FROZEN, UNAVAILABLE, PERMISSION_REQUIRED, ERROR }
data class MagnifierState(val status:MagnifierStatus=MagnifierStatus.STOPPED,val frozen:Bitmap?=null,val zoom:Float=1f,val minZoom:Float=1f,val maxZoom:Float=1f,val torchAvailable:Boolean=false,val torch:Boolean=false)
fun validMagnifierDimensions(width:Int,height:Int)=width>0 && height>0 && width<=4096 && height<=4096 && width.toLong()*height<=2_000_000
class MagnifierExportSelection {
    private var frame:Bitmap?=null
    fun select(value:Bitmap):Boolean {clear();if(value.isRecycled || !validMagnifierDimensions(value.width,value.height))return false;frame=value;return true}
    fun take():Bitmap?=frame.also{frame=null}
    fun clear(){frame=null}
}
fun writeMagnifierPng(frame:Bitmap,output:OutputStream):Boolean = !frame.isRecycled && validMagnifierDimensions(frame.width,frame.height) && frame.compress(Bitmap.CompressFormat.PNG,100,output)
