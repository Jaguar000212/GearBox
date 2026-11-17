package com.jaguar.gearbox.images

import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.floor

const val STUDIO_COLLAGE_SOURCE_PIXELS = 1_000_000L
enum class StudioCollageLayout { HORIZONTAL, VERTICAL, GRID }
enum class StudioImageFit { CONTAIN, COVER }
/** Integer controls in encoded sRGB; zero is identity, not an automatic enhancement. */
data class StudioEnhancement(val brightness: Int = 0, val contrast: Int = 0, val saturation: Int = 0) {
    fun valid() = brightness in -100..100 && contrast in -100..100 && saturation in -100..100
}

fun studioWorkingSize(width: Int, height: Int, maxWorkingPixels: Long): Pair<Int, Int> {
    require(maxWorkingPixels in 1..ImageStudioLimits.MAX_PIXELS)
    val (w,h) = ImageStudioLimits.decodedSize(width,height)
    val scale = min(1.0,sqrt(maxWorkingPixels.toDouble()/(w.toLong()*h)))
    var targetWidth=maxOf(1,floor(w*scale).toInt())
    var targetHeight=maxOf(1,floor(h*scale).toInt())
    if (targetWidth.toLong()*targetHeight > maxWorkingPixels) {
        if (targetWidth >= targetHeight) targetWidth=(maxWorkingPixels/targetHeight).toInt()
        else targetHeight=(maxWorkingPixels/targetWidth).toInt()
    }
    return targetWidth to targetHeight
}

/** Partition integer pixels exactly; grid uses two columns, with an unused background cell for 3. */
fun studioCollageCells(count: Int, width: Int, height: Int, layout: StudioCollageLayout): List<ImageStudioCrop> {
    require(count in 2..4 && ImageStudioLimits.validOutput(width,height))
    val columns = when(layout) { StudioCollageLayout.HORIZONTAL -> count; StudioCollageLayout.VERTICAL -> 1; StudioCollageLayout.GRID -> 2 }
    val rows = (count+columns-1)/columns
    require(width >= columns && height >= rows)
    return List(count) { i ->
        val x=i%columns; val y=i/columns
        ImageStudioCrop(x*width/columns,y*height/rows,(x+1)*width/columns,(y+1)*height/rows)
    }
}

/** Preserves alpha; transparent pixels remain transparent. Luma weights are intentionally applied
 * to encoded sRGB channels, not linear-light colorimetry. Contrast pivots at 127.5, brightness adds
 * up to 255, then saturation interpolates around encoded luma. Clamp/round only at final output. */
fun enhanceStudioPixel(pixel: Int, settings: StudioEnhancement): Int {
    require(settings.valid())
    if (settings.brightness == 0 && settings.contrast == 0 && settings.saturation == 0) return pixel
    val alpha = pixel ushr 24
    if (alpha == 0) return 0
    val factor=1.0+settings.contrast/100.0
    val add=settings.brightness*2.55
    val red=(((pixel ushr 16) and 255)-127.5)*factor+127.5+add
    val green=(((pixel ushr 8) and 255)-127.5)*factor+127.5+add
    val blue=((pixel and 255)-127.5)*factor+127.5+add
    val luma=0.2126*red+0.7152*green+0.0722*blue
    val saturation=1.0+settings.saturation/100.0
    fun channel(value:Double) = floor((luma+(value-luma)*saturation).coerceIn(0.0,255.0)+0.5).toInt()
    return (alpha shl 24) or (channel(red) shl 16) or (channel(green) shl 8) or channel(blue)
}
