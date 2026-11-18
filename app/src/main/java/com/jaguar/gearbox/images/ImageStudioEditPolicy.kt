package com.jaguar.gearbox.images

const val IMAGE_STUDIO_MAX_EDITS = 20
enum class ImageStudioEditKind { RECTANGLE, ARROW, SOLID, BLUR, MOSAIC, TEXT }

/** Working-bitmap coordinates, applied in order before crop/resize. For ARROW only, region fields are inclusive start/end pixel coordinates in any direction.
 * Blur and mosaic are visual effects, not secure redaction. No operation edits the source file. */
data class ImageStudioEdit(
    val kind: ImageStudioEditKind,
    val region: ImageStudioCrop,
    val color: Int = 0xff000000.toInt(),
    val strength: Int = 8,
    val text: String = "",
) {
    fun validFor(width: Int, height: Int): Boolean = (if (kind == ImageStudioEditKind.ARROW)
        region.left in 0 until width && region.right in 0 until width &&
            region.top in 0 until height && region.bottom in 0 until height &&
            (region.left != region.right || region.top != region.bottom)
        else region.validFor(width, height)) && when (kind) {
        ImageStudioEditKind.RECTANGLE -> color ushr 24 == 255 && strength in 1..32
        ImageStudioEditKind.ARROW -> color ushr 24 == 255 && strength in 1..32
        ImageStudioEditKind.SOLID -> color ushr 24 == 255
        ImageStudioEditKind.BLUR -> strength in 1..32
        ImageStudioEditKind.MOSAIC -> strength in 2..128
        ImageStudioEditKind.TEXT -> color ushr 24 == 255 && strength in 8..128 && validStudioAnnotation(text)
    }
}

fun validStudioEdits(edits: List<ImageStudioEdit>, width: Int, height: Int): Boolean =
    ImageStudioLimits.validOutput(width, height) && edits.size <= IMAGE_STUDIO_MAX_EDITS && edits.all { it.validFor(width, height) }

/** Single-line Unicode scalar text, at most 120 code points; never retained as a mode. */
fun validStudioAnnotation(text: String): Boolean {
    if (text.isBlank() || text.length > 240) return false
    var index = 0
    var count = 0
    while (index < text.length) {
        val ch = text[index]
        val codePoint = when {
            Character.isHighSurrogate(ch) -> {
                if (index + 1 >= text.length || !Character.isLowSurrogate(text[index + 1])) return false
                Character.toCodePoint(ch, text[++index])
            }
            Character.isLowSurrogate(ch) -> return false
            else -> ch.code
        }
        if (Character.isISOControl(codePoint) || codePoint == 0x2028 || codePoint == 0x2029) return false
        if (++count > 120) return false
        index++
    }
    return true
}
