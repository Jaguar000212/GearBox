package com.jaguar.gearbox.logic

enum class JsonFormatMode { VALIDATE, PRETTY, MINIFY }
enum class JsonFormatErrorKind { SYNTAX, DUPLICATE_KEY, INVALID_UNICODE, INPUT_LIMIT, DEPTH_LIMIT, TOKEN_LIMIT, OUTPUT_LIMIT }
data class JsonFormatError(val kind: JsonFormatErrorKind, val line: Int, val column: Int)
data class JsonFormatResult(val output: String? = null, val error: JsonFormatError? = null)
const val JSON_INPUT_LIMIT = 64000
const val JSON_OUTPUT_LIMIT = 256000
private class JsonFailure(val kind: JsonFormatErrorKind, val offset: Int) : RuntimeException()
private sealed interface JsonNode
private data class JsonScalar(val raw: String) : JsonNode
private data class JsonArray(val values: List<JsonNode>) : JsonNode
private data class JsonObject(val values: List<Pair<String, JsonNode>>) : JsonNode

/** Strict JSON with Unicode scalar strings and unique decoded object keys; never executes input. */
fun evaluateJson(source: String, mode: JsonFormatMode, indent: Int = 2): JsonFormatResult {
    return try {
        if (source.length > JSON_INPUT_LIMIT) throw JsonFailure(JsonFormatErrorKind.INPUT_LIMIT, JSON_INPUT_LIMIT)
        if (indent != 2 && indent != 4) throw JsonFailure(JsonFormatErrorKind.SYNTAX, 0)
        val node = StrictJsonParser(source).parse()
        if (mode == JsonFormatMode.VALIDATE) JsonFormatResult(output = "")
        else JsonFormatResult(output = JsonWriter(mode == JsonFormatMode.PRETTY, indent).write(node))
    } catch (failure: JsonFailure) {
        var line = 1; var column = 1; var index = 0
        val end = failure.offset.coerceIn(0, source.length)
        while (index < end) {
            when (source[index]) {
                '\r' -> { line++; column = 1; if (index + 1 < end && source[index + 1] == '\n') index++ }
                '\n' -> { line++; column = 1 }
                else -> column++
            }
            index++
        }
        JsonFormatResult(error = JsonFormatError(failure.kind, line, column))
    }
}

private class StrictJsonParser(private val text: String) {
    private var p = 0
    private var tokens = 0
    private fun fail(kind: JsonFormatErrorKind = JsonFormatErrorKind.SYNTAX, at: Int = p): Nothing = throw JsonFailure(kind, at)
    private fun token() { if (++tokens > 20000) fail(JsonFormatErrorKind.TOKEN_LIMIT) }
    private fun space() { while (p < text.length && text[p] in " \t\r\n") p++ }
    private fun take(char: Char): Boolean {
        space()
        if (p < text.length && text[p] == char) { token(); p++; return true }
        return false
    }
    fun parse(): JsonNode {
        val result = value(0)
        space(); if (p != text.length) fail()
        return result
    }
    private fun value(depth: Int): JsonNode {
        space()
        if (p >= text.length) fail()
        return when (text[p]) {
            '{' -> {
                if (depth >= 64) fail(JsonFormatErrorKind.DEPTH_LIMIT)
                take('{')
                val entries = mutableListOf<Pair<String, JsonNode>>()
                val keys = mutableSetOf<String>()
                if (!take('}')) {
                    while (true) {
                        space(); val keyStart = p
                        val (raw, decoded) = string()
                        if (!keys.add(decoded)) fail(JsonFormatErrorKind.DUPLICATE_KEY, keyStart)
                        if (!take(':')) fail()
                        entries += raw to value(depth + 1)
                        if (take('}')) break
                        if (!take(',')) fail()
                    }
                }
                JsonObject(entries)
            }
            '[' -> {
                if (depth >= 64) fail(JsonFormatErrorKind.DEPTH_LIMIT)
                take('[')
                val entries = mutableListOf<JsonNode>()
                if (!take(']')) while (true) {
                    entries += value(depth + 1)
                    if (take(']')) break
                    if (!take(',')) fail()
                }
                JsonArray(entries)
            }
            '"' -> JsonScalar(string().first)
            't' -> literal("true")
            'f' -> literal("false")
            'n' -> literal("null")
            '-', in '0'..'9' -> number()
            else -> fail()
        }
    }
    private fun literal(expected: String): JsonNode {
        if (!text.startsWith(expected, p)) fail()
        token(); p += expected.length; return JsonScalar(expected)
    }
    private fun number(): JsonNode {
        val start = p
        if (text[p] == '-') p++
        if (p >= text.length) fail()
        if (text[p] == '0') p++ else {
            if (text[p] !in '1'..'9') fail()
            while (p < text.length && text[p] in '0'..'9') p++
        }
        if (p < text.length && text[p] == '.') {
            p++; val begin = p
            while (p < text.length && text[p] in '0'..'9') p++
            if (p == begin) fail()
        }
        if (p < text.length && text[p] in "eE") {
            p++; if (p < text.length && text[p] in "+-") p++
            val begin = p
            while (p < text.length && text[p] in '0'..'9') p++
            if (p == begin) fail()
        }
        token(); return JsonScalar(text.substring(start, p))
    }
    private fun string(): Pair<String, String> {
        space(); val start = p
        if (p >= text.length || text[p] != '"') fail()
        p++; token()
        val decoded = StringBuilder()
        val origins = mutableListOf<Int>()
        while (p < text.length) {
            val at = p; val char = text[p++]
            if (char == '"') {
                var index = 0
                while (index < decoded.length) {
                    val unit = decoded[index]
                    if (unit.isHighSurrogate()) {
                        if (index + 1 >= decoded.length || !decoded[index + 1].isLowSurrogate()) fail(JsonFormatErrorKind.INVALID_UNICODE, origins[index])
                        index += 2
                    } else { if (unit.isLowSurrogate()) fail(JsonFormatErrorKind.INVALID_UNICODE, origins[index]); index++ }
                }
                return text.substring(start, p) to decoded.toString()
            }
            if (char.code < 0x20) fail(at = at)
            origins += at
            if (char != '\\') { decoded.append(char); continue }
            if (p >= text.length) fail()
            when (val escape = text[p++]) {
                '"', '\\', '/' -> decoded.append(escape)
                'b' -> decoded.append('\b')
                'f' -> decoded.append('\u000C')
                'n' -> decoded.append('\n')
                'r' -> decoded.append('\r')
                't' -> decoded.append('\t')
                'u' -> {
                    if (p + 4 > text.length) fail()
                    val hex = text.substring(p, p + 4)
                    if (hex.any { it !in "0123456789abcdefABCDEF" }) fail()
                    decoded.append(hex.toInt(16).toChar()); p += 4
                }
                else -> fail(at = p - 1)
            }
        }
        fail()
    }
}

private class JsonWriter(private val pretty: Boolean, private val indent: Int) {
    private val out = StringBuilder()
    private fun append(text: String) {
        if (out.length + text.length > JSON_OUTPUT_LIMIT) throw JsonFailure(JsonFormatErrorKind.OUTPUT_LIMIT, 0)
        out.append(text)
    }
    private fun newline(depth: Int) { if (pretty) { append("\n"); append(" ".repeat(depth * indent)) } }
    fun write(value: JsonNode): String { node(value, 0); return out.toString() }
    private fun node(value: JsonNode, depth: Int) {
        when (value) {
            is JsonScalar -> append(value.raw)
            is JsonArray -> {
                append("[")
                value.values.forEachIndexed { index, child ->
                    if (index > 0) append(",")
                    newline(depth + 1); node(child, depth + 1)
                }
                if (value.values.isNotEmpty()) newline(depth)
                append("]")
            }
            is JsonObject -> {
                append("{")
                value.values.forEachIndexed { index, (key, child) ->
                    if (index > 0) append(",")
                    newline(depth + 1); append(key); append(if (pretty) ": " else ":"); node(child, depth + 1)
                }
                if (value.values.isNotEmpty()) newline(depth)
                append("}")
            }
        }
    }
}
