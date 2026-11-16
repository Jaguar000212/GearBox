package com.jaguar.gearbox.data

/** Validates persisted random outcomes without generating a new outcome or changing a tally.
 * Missing typed fields (older entries) intentionally cannot replay a result. */
object RandomResultSnapshots {
    fun coin(fields: Map<String, String>): Boolean? = fields["_heads"]?.toBooleanStrictOrNull()
    fun bottle(fields: Map<String, String>): Int? = fields["_angle"]?.toIntOrNull()?.takeIf { it in 0..359 }
    fun dice(fields: Map<String, String>): List<Int>? {
        val count = fields["dice"]?.toIntOrNull()?.takeIf { it in 1..6 } ?: return null
        val tokens = fields["_dice"]?.split(',') ?: return null
        if (tokens.size != count) return null
        return tokens.map { it.toIntOrNull()?.takeIf { value -> value in 1..6 } ?: return null }
    }
    fun number(fields: Map<String, String>): Long? {
        val low = fields["min"]?.trim()?.toLongOrNull() ?: return null
        val high = fields["max"]?.trim()?.toLongOrNull() ?: return null
        return fields["_number"]?.toLongOrNull()?.takeIf { low <= high && it in low..high }
    }
    fun choice(fields: Map<String, String>): Int? {
        if (fields["optionA"].isNullOrBlank() || fields["optionB"].isNullOrBlank()) return null
        return fields["_choice"]?.toIntOrNull()?.takeIf { it in 0..1 }
    }
    fun rps(fields: Map<String, String>): Pair<String, String>? {
        val moves = setOf("ROCK", "PAPER", "SCISSORS")
        val player = fields["playerMove"]?.takeIf { it in moves } ?: return null
        val cpu = fields["_cpuMove"]?.takeIf { it in moves } ?: return null
        return player to cpu
    }
}
