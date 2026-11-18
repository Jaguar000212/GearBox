package com.jaguar.gearbox.images

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min

/** Internal: caller holds studioWorkMutex and owns this mutable working copy. */
internal fun applyStudioEdits(bitmap: Bitmap, edits: List<ImageStudioEdit>, checkActive: () -> Unit) {
    require(validStudioEdits(edits, bitmap.width, bitmap.height))
    for (edit in edits) {
        checkActive()
        when (edit.kind) {
            ImageStudioEditKind.BLUR, ImageStudioEditKind.MOSAIC -> pixelEffect(bitmap, edit, checkActive)
            else -> drawMarkup(bitmap, edit)
        }
    }
    checkActive()
}

private fun drawMarkup(bitmap: Bitmap, edit: ImageStudioEdit) {
    val r = edit.region
    val canvas = Canvas(bitmap)
    val saved = canvas.save()
    try {
        if (edit.kind != ImageStudioEditKind.ARROW) canvas.clipRect(r.left, r.top, r.right, r.bottom)
        val paint = Paint().apply { color = edit.color; strokeWidth = edit.strength.toFloat() }
        when (edit.kind) {
            ImageStudioEditKind.TEXT -> {
                paint.isAntiAlias = true
                paint.textSize = edit.strength.toFloat()
                // Input Y is the top of the font metrics, not the baseline. Canvas clipping is intentional.
                canvas.drawText(edit.text, r.left.toFloat(), r.top - paint.fontMetrics.top, paint)
            }
            ImageStudioEditKind.SOLID -> canvas.drawRect(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat(), paint)
            ImageStudioEditKind.RECTANGLE -> {
                val thickness = edit.strength.toFloat()
                canvas.drawRect(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), min(r.bottom.toFloat(), r.top+thickness), paint)
                canvas.drawRect(r.left.toFloat(), maxOf(r.top.toFloat(), r.bottom-thickness), r.right.toFloat(), r.bottom.toFloat(), paint)
                canvas.drawRect(r.left.toFloat(), r.top.toFloat(), min(r.right.toFloat(), r.left+thickness), r.bottom.toFloat(), paint)
                canvas.drawRect(maxOf(r.left.toFloat(), r.right-thickness), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat(), paint)
            }
            ImageStudioEditKind.ARROW -> {
                val x1 = r.left.toFloat(); val y1 = r.top.toFloat()
                val x2 = r.right.toFloat(); val y2 = r.bottom.toFloat()
                canvas.drawLine(x1, y1, x2, y2, paint)
                val angle = atan2(y2-y1, x2-x1)
                val length = min(32f, kotlin.math.hypot(x2-x1, y2-y1) * 0.35f)
                for (delta in listOf(-0.6, 0.6)) canvas.drawLine(x2,y2,
                    x2-length*cos(angle+delta).toFloat(), y2-length*sin(angle+delta).toFloat(), paint)
            }
            else -> error("Not a drawing operation")
        }
    } finally { canvas.restoreToCount(saved) }
}

private fun pixelEffect(bitmap: Bitmap, edit: ImageStudioEdit, checkActive: () -> Unit) {
    val r = edit.region; val width = r.right-r.left; val height = r.bottom-r.top
    val pixels = IntArray(width*height)
    bitmap.getPixels(pixels, 0, width, r.left, r.top, width, height)
    if (edit.kind == ImageStudioEditKind.MOSAIC) {
        val block = edit.strength
        for (top in 0 until height step block) {
            checkActive()
            for (left in 0 until width step block) {
                val bottom = min(height, top+block); val right = min(width, left+block)
                val sums = LongArray(4)
                for (y in top until bottom) for (x in left until right) accumulate(sums, pixels[y*width+x], 1)
                val color = average(sums, (bottom-top)*(right-left))
                for (y in top until bottom) for (x in left until right) pixels[y*width+x] = color
            }
        }
    } else {
        // Separable box blur with sliding sums: O(region pixels), not O(radius * pixels).
        val horizontal = IntArray(pixels.size)
        val radius = edit.strength
        for (y in 0 until height) {
            checkActive()
            val sums = LongArray(4)
            var left = 0; var right = -1
            for (x in 0 until width) {
                val end = min(width-1, x+radius)
                while (right < end) accumulate(sums, pixels[y*width+(++right)], 1)
                val start = maxOf(0, x-radius)
                while (left < start) accumulate(sums, pixels[y*width+(left++)], -1)
                horizontal[y*width+x] = average(sums, right-left+1)
            }
        }
        for (x in 0 until width) {
            checkActive()
            val sums = LongArray(4)
            var top = 0; var bottom = -1
            for (y in 0 until height) {
                val end = min(height-1, y+radius)
                while (bottom < end) accumulate(sums, horizontal[(++bottom)*width+x], 1)
                val start = maxOf(0, y-radius)
                while (top < start) accumulate(sums, horizontal[(top++)*width+x], -1)
                pixels[y*width+x] = average(sums, bottom-top+1)
            }
        }
    }
    checkActive()
    bitmap.setPixels(pixels, 0, width, r.left, r.top, width, height)
}

/** Alpha-weighted RGB avoids pulling hidden transparent RGB into visible edges. */
private fun accumulate(sums: LongArray, color: Int, sign: Int) {
    val alpha = color ushr 24
    sums[0] += alpha.toLong()*sign
    sums[1] += ((color ushr 16) and 255).toLong()*alpha*sign
    sums[2] += ((color ushr 8) and 255).toLong()*alpha*sign
    sums[3] += (color and 255).toLong()*alpha*sign
}
private fun average(sums: LongArray, count: Int): Int {
    val alpha = (sums[0]/count).toInt()
    if (alpha == 0 || sums[0] == 0L) return 0
    return (alpha shl 24) or ((sums[1]/sums[0]).toInt() shl 16) or
        ((sums[2]/sums[0]).toInt() shl 8) or (sums[3]/sums[0]).toInt()
}
