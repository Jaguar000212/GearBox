package com.jaguar.gearbox.data

import com.jaguar.gearbox.logic.parseBusinessDate
import com.jaguar.gearbox.logic.parseBusinessExclusions
import com.jaguar.gearbox.logic.parseBusinessOffset
import com.jaguar.gearbox.logic.parseBusinessWeekendMask

/** Preserve validated source spelling, including exclusion separators, for hydration fingerprints. */
data class BusinessDaySnapshot(
    val mode: String,
    val start: String,
    val end: String,
    val offset: Int,
    val weekendMask: Int,
    val excludedDates: String,
) {
    fun toInputs(): Map<String, String> = mapOf("mode" to mode, "start" to start, "end" to end,
        "offset" to offset.toString(), "weekendMask" to weekendMask.toString(), "excludedDates" to excludedDates)
}

/** Both date fields and the offset are validated, even when one is inactive in the chosen mode. */
fun parseBusinessDaySnapshot(values: Map<String, String>): BusinessDaySnapshot? {
    if (values.keys != setOf("mode", "start", "end", "offset", "weekendMask", "excludedDates")) return null
    val mode = values.getValue("mode")
    if (mode !in setOf("ADD", "COUNT")) return null
    val start = values.getValue("start")
    val end = values.getValue("end")
    if (parseBusinessDate(start) == null || parseBusinessDate(end) == null) return null
    val offset = parseBusinessOffset(values.getValue("offset")) ?: return null
    val mask = parseBusinessWeekendMask(values.getValue("weekendMask")) ?: return null
    if (offset.toString() != values["offset"] || mask.toString() != values["weekendMask"]) return null
    val exclusions = values.getValue("excludedDates")
    if (parseBusinessExclusions(exclusions) == null) return null
    return BusinessDaySnapshot(mode, start, end, offset, mask, exclusions)
}

/** Mode preference cannot retain personal dates or a holiday/exclusion list. */
fun validBusinessDayMode(values: Map<String, String>): Boolean =
    values.keys == setOf("mode", "weekendMask") && values["mode"] in setOf("ADD", "COUNT") &&
        parseBusinessWeekendMask(values.getValue("weekendMask"))?.toString() == values["weekendMask"]
