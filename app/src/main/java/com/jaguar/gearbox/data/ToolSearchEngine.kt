package com.jaguar.gearbox.data

import java.text.Normalizer
import java.util.Locale

/** Pure matching model. Operations contain selectors only, never executable instructions. */
data class SearchOperationDocument(val id: String, val terms: List<String>)
data class ToolSearchDocument(val route: String, val terms: List<String>, val operations: List<SearchOperationDocument> = emptyList(), val genericTerms: List<String> = emptyList())
data class RankedToolMatch(val route: String, val operationId: String?, val score: Int)

object ToolSearchEngine {
    private val ignoredQuestionWords = setOf("a", "an", "the", "is", "this", "that", "it", "i", "my", "me", "do", "does", "how", "can", "to", "please", "want", "need", "what")
    fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")

    fun rank(query: String, documents: List<ToolSearchDocument>): List<RankedToolMatch> {
        if (query.isBlank()) return documents.map { RankedToolMatch(it.route, null, 0) }
        val normalized = normalize(query)
        val tokens = normalized.split(' ').filter { it.isNotBlank() && it !in ignoredQuestionWords }
        if (tokens.isEmpty()) return emptyList()
        val phrase = tokens.joinToString(" ")
        return documents.mapNotNull { doc ->
            val base = score(phrase, tokens, doc.terms, normalized)
            val candidates = doc.operations.map { op ->
                // A generic tool/category match must not silently choose its first mode.
                val ownScore = score(phrase, tokens, op.terms, normalized)
                val ownTokens = op.terms.flatMap { normalize(it).split(' ') }.toSet()
                val relevant = tokens.any { token -> ownTokens.any { it.startsWith(token) } }
                val combined = if (relevant) score(phrase, tokens, op.terms + doc.terms, normalized) else 0
                op.id to maxOf(ownScore, combined)
            }
            val best = candidates.maxOfOrNull { it.second } ?: 0
            val winners = candidates.filter { it.second == best && best > 0 }
            val exactGeneric = doc.genericTerms.any { normalize(it) == phrase }
            val operation = winners.singleOrNull()?.takeIf { best >= base && !exactGeneric }?.first
            val total = maxOf(base, best)
            if (total <= 0) null else RankedToolMatch(doc.route, operation, total)
        }.sortedByDescending { it.score }
    }

    private fun score(phrase: String, tokens: List<String>, fields: List<String>, rawPhrase: String): Int {
        val normalized = fields.map(::normalize).filter(String::isNotBlank)
        // Preserve direction words/order for exact operations (text to binary != binary to text).
        if (normalized.any { it == rawPhrase }) return 1100
        if (normalized.any { it == phrase }) return 1000
        if (normalized.any { it.startsWith(phrase) }) return 850
        if (normalized.any { field -> tokens.all { token -> field.split(' ').any { it == token } } }) return 700
        val words = normalized.flatMap { it.split(' ') }
        if (tokens.all { it in words }) return 600
        // Prefixes support typing, but require every meaningful word rather than substring OR.
        if (tokens.all { token -> words.any { it.startsWith(token) } }) return 350
        return 0
    }
}
