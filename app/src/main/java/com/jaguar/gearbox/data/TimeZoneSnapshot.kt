package com.jaguar.gearbox.data

import com.jaguar.gearbox.logic.resolveTimeZoneSource
import com.jaguar.gearbox.logic.validConversionZone

/** Event-specific values stay outside mode preferences. Restore never performs a conversion. */
data class TimeZoneSnapshot(val date: String, val time: String, val sourceZone: String,
    val targetZone: String, val sourceOffset: String) {
    fun toInputs(): Map<String, String> = mapOf("date" to date, "time" to time, "sourceZone" to sourceZone,
        "targetZone" to targetZone, "sourceOffset" to sourceOffset)
}

/** Device TZDB validates gaps and literal overlap offsets. AUTO on an overlap remains an inert
 * pending choice: the conversion engine must still require an explicit selection before output.
 */
fun parseTimeZoneSnapshot(values: Map<String, String>): TimeZoneSnapshot? {
    if (values.keys != setOf("date", "time", "sourceZone", "targetZone", "sourceOffset")) return null
    val date = values.getValue("date")
    val time = values.getValue("time")
    val sourceZone = values.getValue("sourceZone")
    val targetZone = values.getValue("targetZone")
    val offset = values.getValue("sourceOffset")
    if (!validConversionZone(targetZone)) return null
    val source = resolveTimeZoneSource(date, time, sourceZone)
    if (source.error != null) return null
    val validOffset = when (source.offsets.size) {
        1 -> offset == "AUTO"
        2 -> offset == "AUTO" || source.offsets.any { it.id == offset }
        else -> false
    }
    if (!validOffset) return null
    return TimeZoneSnapshot(date, time, sourceZone, targetZone, offset)
}

fun validTimeZoneMode(values: Map<String, String>): Boolean =
    values.keys == setOf("sourceZone", "targetZone") &&
        validConversionZone(values.getValue("sourceZone")) && validConversionZone(values.getValue("targetZone"))
