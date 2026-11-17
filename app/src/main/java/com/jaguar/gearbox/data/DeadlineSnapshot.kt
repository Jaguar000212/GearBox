package com.jaguar.gearbox.data

import com.jaguar.gearbox.logic.DeadlinePhase
import com.jaguar.gearbox.logic.parseDeadlineDate
import com.jaguar.gearbox.logic.validDeadlineLabel

/** Saved as-of date is explicit input, never replaced with today's date during hydration. */
data class DeadlineSnapshot(val deadline: String, val asOf: String, val phases: List<DeadlinePhase>) {
    fun toInputs(): Map<String, String> = mapOf("deadline" to deadline, "asOf" to asOf,
        "ids" to phases.joinToString(",") { it.id.toString() },
        "names" to phases.joinToString("\n") { it.label },
        "durations" to phases.joinToString(",") { it.durationDays.toString() })
}

/** Strict compact schema; labels retain exact spelling and canonical integers round-trip unchanged. */
fun parseDeadlineSnapshot(values: Map<String, String>): DeadlineSnapshot? {
    if (values.keys != setOf("deadline", "asOf", "ids", "names", "durations") || !ToolMemoryPolicy.validInputs(values)) return null
    val deadline = values.getValue("deadline")
    val asOf = values.getValue("asOf")
    val target = parseDeadlineDate(deadline) ?: return null
    if (parseDeadlineDate(asOf) == null) return null
    val ids = values.getValue("ids").split(',')
    val names = values.getValue("names").split('\n')
    val durations = values.getValue("durations").split(',')
    if (ids.size !in 1..20 || names.size != ids.size || durations.size != ids.size) return null
    val phases = ids.indices.map { index ->
        val id = ids[index].takeIf { Regex("[1-9][0-9]{0,9}").matches(it) }?.toIntOrNull()
            ?.takeIf { it in 1..1_000_000_000 } ?: return null
        val days = durations[index].takeIf { Regex("[1-9][0-9]{0,3}").matches(it) }?.toIntOrNull()
            ?.takeIf { it in 1..3650 } ?: return null
        val name = names[index].takeIf(::validDeadlineLabel) ?: return null
        DeadlinePhase(id, name, days)
    }
    if (phases.map { it.id }.toSet().size != phases.size) return null
    val total = phases.sumOf { it.durationDays }
    if (total > 36500 || target.minusDays(total.toLong() - 1).year < 1900) return null
    return DeadlineSnapshot(deadline, asOf, phases)
}
