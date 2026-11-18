package com.jaguar.gearbox.data

/** New history/result memory is opt-in; legacy game-session retention stays on by default. */
data class ToolMemorySettings(
    val historyEnabled: Boolean = false,
    val historyLimit: Int = 20,
    val rememberModes: Boolean = false,
    val rememberResults: Boolean = false,
    /** Preserve legacy behavior for in-progress games; independent of result/history opt-ins. */
    val rememberGameSessions: Boolean = true,
    /** Per-route automatic capture consent; absent sensitive routes are OFF, including upgrades. */
    val automaticMemory: Map<String, ToolAutomaticMemory> = emptyMap(),
) {
    fun normalized() = copy(historyLimit = historyLimit.coerceIn(MIN_HISTORY, MAX_HISTORY),
        automaticMemory = automaticMemory.filterKeys(ToolMemoryPolicy::permitsRoute))
    fun automaticMemoryFor(route: String): ToolAutomaticMemory = when {
        !ToolMemoryPolicy.permitsRoute(route) -> ToolAutomaticMemory()
        else -> automaticMemory[route] ?: if (ToolMemoryPolicy.isSensitiveTextRoute(route))
            ToolAutomaticMemory() else ToolAutomaticMemory(true, true)
    }
    fun permitsHistory(route: String): Boolean = historyEnabled && automaticMemoryFor(route).historyEnabled
    fun permitsResult(route: String): Boolean = rememberResults && automaticMemoryFor(route).rememberResults
    companion object {
        const val MIN_HISTORY = 1
        const val MAX_HISTORY = 100
    }
}

data class ToolAutomaticMemory(
    val historyEnabled: Boolean = false,
    val rememberResults: Boolean = false,
)

data class ToolHistoryEntry(
    val id: String,
    val timestampMillis: Long,
    val inputs: Map<String, String>,
    val output: String,
)

/** An explicitly saved setup or workbench result, independent of automatic history. */
data class SavedToolItem(
    val id: String,
    val name: String,
    val route: String,
    val timestampMillis: Long,
    val inputs: Map<String, String>,
    val output: String = "",
)

/** Bounds both the number and size of records. Oversized input is rejected, never truncated. */
object ToolMemoryPolicy {
    const val MAX_FIELDS = 24
    const val MAX_FIELD_CHARS = 4096
    const val MAX_RECORD_CHARS = 8192
    const val MAX_FILE_BYTES = 4 * 1024 * 1024
    private val routePattern = Regex("tool/[a-z0-9_]{1,64}")

    fun permitsRoute(route: String): Boolean = routePattern.matches(route) &&
        route != "tool/password_generator" && route != "tool/focus_timer" && route != "tool/image_studio" && route != "tool/light_meter" && route != "tool/sound_level_meter" && route != "tool/pitch_tuner" && route != "tool/battery_information" && route != "tool/barometer" && route != "tool/proximity_checker" && route != "tool/sensor_checker" && route != "tool/magnifier" && route != "tool/barcode_scanner"

    /** Image Studio supports only safe output preferences, never capture or named saved items. */
    fun permitsModeRoute(route: String): Boolean = permitsRoute(route) || route == "tool/image_studio"

    val sensitiveTextRoutes: Set<String> = setOf("tool/text_tools", "tool/qr_generator", "tool/hash_generator", "tool/text_diff", "tool/decision_matrix", "tool/bill_splitter", "tool/deadline_planner", "tool/json_formatter")
    fun isSensitiveTextRoute(route: String): Boolean = route in sensitiveTextRoutes

    /** Sensitive tools may remember only enumerated non-content selectors, never text. */
    fun validMode(route: String, values: Map<String, String>): Boolean = validInputs(values) && when (route) {
        "tool/text_tools" -> values.keys.all { it == "case" } && values.values.all {
            value -> com.jaguar.gearbox.logic.TextCase.entries.any { it.name == value }
        }
        "tool/hash_generator" -> values.keys.all { it == "algorithm" } && values.values.all {
            value -> com.jaguar.gearbox.logic.TextHashAlgorithm.entries.any { it.name == value }
        }
        "tool/qr_generator", "tool/decision_matrix", "tool/deadline_planner" -> values.isEmpty()
        "tool/text_diff" -> validTextDiffMode(values)
        "tool/image_studio" -> validImageStudioMode(values)
        "tool/color_blend" -> parseColorBlendSnapshot(values) != null
        "tool/bill_splitter" -> validBillSplitterMode(values)
        "tool/business_day_planner" -> validBusinessDayMode(values)
        "tool/scientific_calculator" -> validScientificMode(values)
        "tool/json_formatter" -> validJsonFormatterMode(values)
        "tool/time_zone_converter" -> validTimeZoneMode(values)
        "tool/aspect_ratio" -> validAspectRatioMode(values)
        else -> permitsRoute(route)
    }

    fun validInputs(inputs: Map<String, String>): Boolean = inputs.size <= MAX_FIELDS &&
        inputs.all { (key, value) -> key.isNotBlank() && key.length <= 64 && value.length <= MAX_FIELD_CHARS } &&
        inputs.entries.sumOf { it.key.length + it.value.length } <= MAX_RECORD_CHARS

    fun validRecord(inputs: Map<String, String>, output: String): Boolean =
        validInputs(inputs) && output.length <= MAX_FIELD_CHARS &&
            inputs.entries.sumOf { it.key.length + it.value.length } + output.length <= MAX_RECORD_CHARS
}

/** Live readings and hardware state are snapshots, not commands that can be replayed. */
fun canRestoreToolSnapshot(route: String): Boolean = ToolMemoryPolicy.permitsRoute(route) && route !in setOf(
    "tool/bubble_level", "tool/compass", "tool/screen_ruler", "tool/flashlight",
)
