package com.jaguar.gearbox.data

import com.jaguar.gearbox.logic.parsePixelDimension
import com.jaguar.gearbox.logic.parseSizingPercent

private val aspectModes = setOf("RATIO", "WIDTH", "HEIGHT", "PERCENT", "FIT", "FILL")

data class AspectRatioSnapshot(val mode: String, val sourceWidth: String, val sourceHeight: String,
    val targetWidth: String, val targetHeight: String, val percent: String) {
    fun toInputs(): Map<String, String> = aspectRatioInputs(mode, sourceWidth, sourceHeight, targetWidth, targetHeight, percent)
}

/** Inactive drafts stay in the editor, not history. No hidden invalid field blocks the active mode. */
fun aspectRatioInputs(mode: String, sourceWidth: String, sourceHeight: String,
    targetWidth: String, targetHeight: String, percent: String): Map<String, String> = mapOf(
    "mode" to mode, "sourceWidth" to sourceWidth, "sourceHeight" to sourceHeight,
    "targetWidth" to if (mode in setOf("WIDTH", "FIT", "FILL")) targetWidth else "",
    "targetHeight" to if (mode in setOf("HEIGHT", "FIT", "FILL")) targetHeight else "",
    "percent" to if (mode == "PERCENT") percent else "",
)

/** Canonical empty inactive fields and untouched active spelling keep hydration fingerprints stable. */
fun parseAspectRatioSnapshot(values: Map<String, String>): AspectRatioSnapshot? {
    if (values.keys != setOf("mode", "sourceWidth", "sourceHeight", "targetWidth", "targetHeight", "percent")) return null
    val mode = values.getValue("mode")
    if (mode !in aspectModes) return null
    val sw = values.getValue("sourceWidth"); val sh = values.getValue("sourceHeight")
    val tw = values.getValue("targetWidth"); val th = values.getValue("targetHeight"); val percent = values.getValue("percent")
    if (parsePixelDimension(sw) == null || parsePixelDimension(sh) == null) return null
    if (mode in setOf("WIDTH", "FIT", "FILL")) { if (parsePixelDimension(tw) == null) return null }
    else if (tw.isNotEmpty()) return null
    if (mode in setOf("HEIGHT", "FIT", "FILL")) { if (parsePixelDimension(th) == null) return null }
    else if (th.isNotEmpty()) return null
    if (mode == "PERCENT") { if (parseSizingPercent(percent) == null) return null }
    else if (percent.isNotEmpty()) return null
    return AspectRatioSnapshot(mode, sw, sh, tw, th, percent)
}

fun validAspectRatioMode(values: Map<String, String>): Boolean = values.keys == setOf("mode") && values["mode"] in aspectModes
