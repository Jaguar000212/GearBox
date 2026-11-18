package com.jaguar.gearbox.images

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext
import kotlin.math.min
import kotlin.math.max

internal fun applyStudioEnhancement(bitmap: Bitmap, settings: StudioEnhancement, checkActive: () -> Unit) {
    require(settings.valid())
    if (settings == StudioEnhancement()) { checkActive(); return }
    val row=IntArray(bitmap.width)
    for (y in 0 until bitmap.height) {
        checkActive()
        bitmap.getPixels(row,0,bitmap.width,0,y,bitmap.width,1)
        for (x in row.indices) row[x]=enhanceStudioPixel(row[x],settings)
        bitmap.setPixels(row,0,bitmap.width,0,y,bitmap.width,1)
    }
    checkActive()
}

/** Contains letterboxes on an opaque background; cover explicitly center-crops each selected image.
 * Source bitmaps must remain alive until completion. Never modifies or recycles them. */
suspend fun collageStudioImages(sources: List<Bitmap>, width: Int, height: Int,
    layout: StudioCollageLayout, fit: StudioImageFit, background: Int = 0xffffffff.toInt(),
    enhancement: StudioEnhancement = StudioEnhancement()): Bitmap {
    val images=sources.toList()
    var pending:Bitmap?=null
    try { return withContext(Dispatchers.Default) { studioWorkMutex.withLock {
        require(images.size in 2..4 && background ushr 24 == 255 && enhancement.valid())
        require(images.all { !it.isRecycled && ImageStudioLimits.validOutput(it.width,it.height) &&
            it.width.toLong()*it.height <= STUDIO_COLLAGE_SOURCE_PIXELS })
        val cells=studioCollageCells(images.size,width,height,layout)
        coroutineContext.ensureActive()
        val result=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        pending=result
        val canvas=Canvas(result); canvas.drawColor(background)
        val paint=Paint(Paint.FILTER_BITMAP_FLAG)
        images.forEachIndexed { index,image ->
            coroutineContext.ensureActive()
            val cell=cells[index]
            val cw=cell.right-cell.left; val ch=cell.bottom-cell.top
            val scale=if(fit==StudioImageFit.CONTAIN) min(cw.toFloat()/image.width,ch.toFloat()/image.height)
                else max(cw.toFloat()/image.width,ch.toFloat()/image.height)
            val dw=image.width*scale; val dh=image.height*scale
            val left=cell.left+(cw-dw)/2f; val top=cell.top+(ch-dh)/2f
            val save=canvas.save()
            try {
                canvas.clipRect(cell.left,cell.top,cell.right,cell.bottom)
                canvas.drawBitmap(image,Rect(0,0,image.width,image.height),RectF(left,top,left+dw,top+dh),paint)
            } finally { canvas.restoreToCount(save) }
        }
        val active=coroutineContext
        applyStudioEnhancement(result,enhancement) { active.ensureActive() }
        result
    } } } catch(error:Throwable) { pending?.let { if(!it.isRecycled) it.recycle() }; throw error }
}
