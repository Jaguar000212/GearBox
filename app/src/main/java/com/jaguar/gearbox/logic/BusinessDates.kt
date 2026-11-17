package com.jaguar.gearbox.logic

import java.time.LocalDate
import java.time.format.DateTimeParseException

val BUSINESS_DATE_MIN: LocalDate = LocalDate.of(1900, 1, 1)
val BUSINESS_DATE_MAX: LocalDate = LocalDate.of(2100, 12, 31)
fun parseBusinessDate(text: String): LocalDate? {
    if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(text)) return null
    return try { LocalDate.parse(text).takeIf { it in BUSINESS_DATE_MIN..BUSINESS_DATE_MAX } }
    catch (_: DateTimeParseException) { null }
}
fun parseBusinessOffset(text: String): Int? = text.takeIf { Regex("0|-?[1-9][0-9]{0,4}").matches(it) }
    ?.toIntOrNull()?.takeIf { it in -10000..10000 }
fun parseBusinessWeekendMask(text: String): Int? = text.takeIf { Regex("0|[1-9][0-9]{0,2}").matches(it) }
    ?.toIntOrNull()?.takeIf { it in 0..126 }
fun parseBusinessExclusions(text: String): Set<LocalDate>? {
    if (text.length > 1200) return null
    if (text.isBlank()) return emptySet()
    val tokens = text.replace("\r\n", "\n").replace('\r', '\n').split(',', '\n')
    if (tokens.size > 100) return null
    val dates = tokens.map { parseBusinessDate(it.trim()) ?: return null }
    return dates.toSet()
}
private fun validateBusinessInputs(start: LocalDate, mask: Int, excluded: Set<LocalDate>) {
    require(start in BUSINESS_DATE_MIN..BUSINESS_DATE_MAX && mask in 0..126)
    require(excluded.size <= 100 && excluded.all { it in BUSINESS_DATE_MIN..BUSINESS_DATE_MAX })
}
private fun isBusinessDate(date: LocalDate, mask: Int, excluded: Set<LocalDate>): Boolean =
    mask and (1 shl (date.dayOfWeek.value - 1)) == 0 && date !in excluded

/** Start is excluded. Zero returns start unchanged, including when start is not a business day. */
fun businessDateOffset(start: LocalDate, offset: Int, weekendMask: Int, excluded: Set<LocalDate>): LocalDate {
    validateBusinessInputs(start, weekendMask, excluded)
    require(offset in -10000..10000)
    var date = start
    var remaining = kotlin.math.abs(offset)
    val direction = if (offset < 0) -1L else 1L
    while (remaining > 0) {
        date = date.plusDays(direction)
        require(date in BUSINESS_DATE_MIN..BUSINESS_DATE_MAX) { "Result exceeds supported date range" }
        if (isBusinessDate(date, weekendMask, excluded)) remaining--
    }
    return date
}

/** Traverses toward end, excluding start and including end. Reverse traversal produces a negative count.
 * Swapping endpoints need not give exact negation when their business-day eligibility differs.
 */
fun countBusinessDates(start: LocalDate, end: LocalDate, weekendMask: Int, excluded: Set<LocalDate>): Int {
    validateBusinessInputs(start, weekendMask, excluded)
    require(end in BUSINESS_DATE_MIN..BUSINESS_DATE_MAX)
    val direction = if (end < start) -1 else 1
    var date = start
    var count = 0
    while (date != end) {
        date = date.plusDays(direction.toLong())
        if (isBusinessDate(date, weekendMask, excluded)) count += direction
    }
    return count
}
