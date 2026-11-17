package com.jaguar.gearbox.logic

const val MAX_BATCH_LINES = 100
const val MAX_BATCH_CHARACTERS = 4096

enum class BatchInputError { TOO_LONG, TOO_MANY_LINES }
enum class BatchRowError { INVALID_NUMBER, OUT_OF_RANGE }
data class BatchConversionRow(val line: Int, val input: String, val value: Double?, val result: Double?, val error: BatchRowError?)
data class BatchConversion(val rows: List<BatchConversionRow>, val error: BatchInputError? = null)

fun batchInputError(text: String): BatchInputError? {
    if (text.length > MAX_BATCH_CHARACTERS) return BatchInputError.TOO_LONG
    if (text.replace("\r\n", "\n").replace('\r', '\n').count { it == '\n' } + 1 > MAX_BATCH_LINES)
        return BatchInputError.TOO_MANY_LINES
    return null
}

/** One row per submitted line, including blanks; never omits, truncates or converts invalid input. */
fun convertNumericBatch(text: String, convert: (Double) -> Double): BatchConversion {
    batchInputError(text)?.let { return BatchConversion(emptyList(), it) }
    return BatchConversion(text.replace("\r\n", "\n").replace('\r', '\n').split('\n').mapIndexed { index, raw ->
        val value = raw.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        if (value == null) BatchConversionRow(index + 1, raw, null, null, BatchRowError.INVALID_NUMBER)
        else {
            val converted = convert(value).takeIf { it.isFinite() }
            BatchConversionRow(index + 1, raw, value, converted, if (converted == null) BatchRowError.OUT_OF_RANGE else null)
        }
    })
}

/** A finite round-trip editor value, not the potentially rounded display string. */
fun reverseConversionInput(converted: Double): String? = converted.takeIf { it.isFinite() }?.toString()
