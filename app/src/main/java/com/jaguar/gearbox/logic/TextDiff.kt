package com.jaguar.gearbox.logic

enum class TextDiffKind { EQUAL, REMOVED, ADDED }
data class TextDiffLine(val kind: TextDiffKind, val text: String)
enum class TextDiffError { TOO_LONG, TOO_MANY_LINES }
data class TextDiffResult(val lines: List<TextDiffLine> = emptyList(), val error: TextDiffError? = null) {
    val added: Int get() = lines.count { it.kind == TextDiffKind.ADDED }
    val removed: Int get() = lines.count { it.kind == TextDiffKind.REMOVED }
    val equal: Int get() = lines.count { it.kind == TextDiffKind.EQUAL }
}
const val TEXT_DIFF_MAX_CHARS = 16000
const val TEXT_DIFF_MAX_LINES = 200

/** Normalize newline conventions only; preserve case, whitespace and a final empty line. */
fun textDiffLines(text: String): List<String> = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
fun validateTextDiffInput(text: String): TextDiffError? = when {
    text.length > TEXT_DIFF_MAX_CHARS -> TextDiffError.TOO_LONG
    textDiffLines(text).size > TEXT_DIFF_MAX_LINES -> TextDiffError.TOO_MANY_LINES
    else -> null
}

/** At most 40,000 LCS comparisons, with a 201x201 boundary-inclusive table. No regex/eval/network. */
fun compareTextLines(first: String, second: String): TextDiffResult {
    val error = validateTextDiffInput(first) ?: validateTextDiffInput(second)
    if (error != null) return TextDiffResult(error = error)
    val a = textDiffLines(first); val b = textDiffLines(second)
    val width = b.size + 1
    val lcs = IntArray((a.size + 1) * width)
    for (i in a.lastIndex downTo 0) for (j in b.lastIndex downTo 0) {
        lcs[i * width + j] = if (a[i] == b[j]) lcs[(i + 1) * width + j + 1] + 1
        else maxOf(lcs[(i + 1) * width + j], lcs[i * width + j + 1])
    }
    val rows = mutableListOf<TextDiffLine>()
    var i = 0; var j = 0
    while (i < a.size || j < b.size) {
        when {
            i < a.size && j < b.size && a[i] == b[j] -> { rows += TextDiffLine(TextDiffKind.EQUAL, a[i]); i++; j++ }
            i < a.size && (j == b.size || lcs[(i + 1) * width + j] >= lcs[i * width + j + 1]) -> {
                rows += TextDiffLine(TextDiffKind.REMOVED, a[i++])
            }
            else -> rows += TextDiffLine(TextDiffKind.ADDED, b[j++])
        }
    }
    return TextDiffResult(rows)
}
