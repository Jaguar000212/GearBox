package com.jaguar.gearbox.logic

import java.time.*
import java.time.format.DateTimeFormatter

val CONVERSION_ZONE_IDS: Set<String> = (ZoneId.getAvailableZoneIds() + "UTC").toSortedSet()
enum class TimeZoneConversionError { INVALID_DATE, INVALID_TIME, INVALID_ZONE, GAP, OVERLAP_CHOICE_REQUIRED, INVALID_OFFSET, OUT_OF_RANGE }
data class TimeZoneSourceResolution(val local: LocalDateTime? = null, val zone: ZoneId? = null,
    val offsets: List<ZoneOffset> = emptyList(), val error: TimeZoneConversionError? = null)
data class TimeZoneConversionResult(val source: ZonedDateTime? = null, val target: ZonedDateTime? = null,
    val error: TimeZoneConversionError? = null)
fun parseConversionDate(text: String): LocalDate? {
    if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(text)) return null
    return try { LocalDate.parse(text).takeIf { it.year in 1900..2100 } } catch (_: DateTimeException) { null }
}
fun parseConversionTime(text: String): LocalTime? {
    if (!Regex("[0-9]{2}:[0-9]{2}").matches(text)) return null
    return try { LocalTime.parse(text) } catch (_: DateTimeException) { null }
}
fun validConversionZone(text: String): Boolean = text in CONVERSION_ZONE_IDS
fun resolveTimeZoneSource(date: String, time: String, sourceZone: String): TimeZoneSourceResolution {
    val day = parseConversionDate(date) ?: return TimeZoneSourceResolution(error = TimeZoneConversionError.INVALID_DATE)
    val clock = parseConversionTime(time) ?: return TimeZoneSourceResolution(error = TimeZoneConversionError.INVALID_TIME)
    if (!validConversionZone(sourceZone)) return TimeZoneSourceResolution(error = TimeZoneConversionError.INVALID_ZONE)
    val zone = ZoneId.of(sourceZone)
    val local = LocalDateTime.of(day, clock)
    val offsets = zone.rules.getValidOffsets(local)
    return TimeZoneSourceResolution(local, zone, offsets, if (offsets.isEmpty()) TimeZoneConversionError.GAP else null)
}
/** Never silently shifts a DST gap or chooses one side of an overlap. Offset selection is literal. */
fun convertTimeZone(date: String, time: String, sourceZone: String, targetZone: String, sourceOffset: String): TimeZoneConversionResult {
    val source = resolveTimeZoneSource(date, time, sourceZone)
    source.error?.let { return TimeZoneConversionResult(error = it) }
    if (!validConversionZone(targetZone)) return TimeZoneConversionResult(error = TimeZoneConversionError.INVALID_ZONE)
    val chosen = when (source.offsets.size) {
        1 -> if (sourceOffset == "AUTO") source.offsets.single() else return TimeZoneConversionResult(error = TimeZoneConversionError.INVALID_OFFSET)
        2 -> source.offsets.firstOrNull { it.id == sourceOffset }
            ?: return TimeZoneConversionResult(error = if (sourceOffset == "AUTO") TimeZoneConversionError.OVERLAP_CHOICE_REQUIRED else TimeZoneConversionError.INVALID_OFFSET)
        else -> return TimeZoneConversionResult(error = TimeZoneConversionError.GAP)
    }
    val resolved = ZonedDateTime.ofStrict(requireNotNull(source.local), chosen, requireNotNull(source.zone))
    val target = resolved.withZoneSameInstant(ZoneId.of(targetZone))
    if (target.year !in 1900..2100) return TimeZoneConversionResult(error = TimeZoneConversionError.OUT_OF_RANGE)
    return TimeZoneConversionResult(resolved, target)
}
fun conversionLocalText(value: ZonedDateTime): String = value.format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss", java.util.Locale.ROOT))
