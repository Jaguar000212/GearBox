package com.jaguar.gearbox.logic

import java.time.LocalDate
import java.time.DateTimeException
import java.time.temporal.ChronoUnit

data class DeadlinePhase(val id: Int, val label: String, val durationDays: Int)
data class PlannedDeadlinePhase(val id: Int, val label: String, val durationDays: Int,
    val start: LocalDate, val end: LocalDate, val daysUntilFinish: Long)
enum class DeadlinePlanError { INVALID_INPUT, OUT_OF_RANGE }
data class DeadlinePlanResult(val phases: List<PlannedDeadlinePhase> = emptyList(), val earliestStart: LocalDate? = null,
    val totalDays: Int = 0, val error: DeadlinePlanError? = null)
fun parseDeadlineDate(text: String): LocalDate? {
    if (!Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(text)) return null
    return try { LocalDate.parse(text).takeIf { it.year in 1900..2100 } } catch (_: DateTimeException) { null }
}
fun validDeadlineLabel(label: String): Boolean = label.isNotBlank() && label.length <= 60 &&
    label.none { it.isISOControl() || it == '\u2028' || it == '\u2029' }
/** Sequential calendar ranges are inclusive: the previous phase ends one day before the next starts. */
fun planBackwardDeadline(deadline: LocalDate, asOf: LocalDate, phases: List<DeadlinePhase>): DeadlinePlanResult {
    if (deadline.year !in 1900..2100 || asOf.year !in 1900..2100 || phases.size !in 1..20 ||
        phases.map { it.id }.toSet().size != phases.size || phases.any { it.id !in 1..1000000000 || !validDeadlineLabel(it.label) || it.durationDays !in 1..3650 })
        return DeadlinePlanResult(error = DeadlinePlanError.INVALID_INPUT)
    val total = phases.sumOf { it.durationDays }
    if (total > 36500) return DeadlinePlanResult(error = DeadlinePlanError.INVALID_INPUT)
    var end = deadline
    val backwards = mutableListOf<PlannedDeadlinePhase>()
    for (phase in phases.asReversed()) {
        val start = end.minusDays(phase.durationDays - 1L)
        if (start.year !in 1900..2100) return DeadlinePlanResult(error = DeadlinePlanError.OUT_OF_RANGE)
        backwards += PlannedDeadlinePhase(phase.id, phase.label, phase.durationDays, start, end, ChronoUnit.DAYS.between(asOf, end))
        end = start.minusDays(1)
    }
    val planned = backwards.asReversed()
    return DeadlinePlanResult(planned, planned.first().start, total)
}
