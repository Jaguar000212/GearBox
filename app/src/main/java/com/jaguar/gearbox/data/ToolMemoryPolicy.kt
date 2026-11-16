package com.jaguar.gearbox.data

/** Local memory is opt-in. Changing the limit never accepts an unbounded value. */
data class ToolMemorySettings(
    val historyEnabled: Boolean = false,
    val historyLimit: Int = 20,
    val rememberModes: Boolean = false,
    val rememberResults: Boolean = false,
) {
    fun normalized() = copy(historyLimit = historyLimit.coerceIn(MIN_HISTORY, MAX_HISTORY))
    companion object {
        const val MIN_HISTORY = 1
        const val MAX_HISTORY = 100
    }
}

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
        route != "tool/password_generator"

    fun validInputs(inputs: Map<String, String>): Boolean = inputs.size <= MAX_FIELDS &&
        inputs.all { (key, value) -> key.isNotBlank() && key.length <= 64 && value.length <= MAX_FIELD_CHARS } &&
        inputs.entries.sumOf { it.key.length + it.value.length } <= MAX_RECORD_CHARS

    fun validRecord(inputs: Map<String, String>, output: String): Boolean =
        validInputs(inputs) && output.length <= MAX_FIELD_CHARS &&
            inputs.entries.sumOf { it.key.length + it.value.length } + output.length <= MAX_RECORD_CHARS
}
