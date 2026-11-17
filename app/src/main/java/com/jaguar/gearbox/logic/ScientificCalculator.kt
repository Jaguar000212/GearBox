package com.jaguar.gearbox.logic

import kotlin.math.*

enum class ScientificAngleMode { DEG, RAD }
enum class ScientificError { SYNTAX, LIMIT, DOMAIN, DIVISION_BY_ZERO, NON_FINITE }
data class ScientificEvaluation(val value: Double? = null, val error: ScientificError? = null)
private class ScientificFailure(val reason: ScientificError) : RuntimeException()

/** Bounded grammar, never evaluation of host-language code. No variables, implicit multiplication or IO. */
fun evaluateScientific(expression: String, mode: ScientificAngleMode): ScientificEvaluation = try {
    if (expression.length > 512) throw ScientificFailure(ScientificError.LIMIT)
    ScientificEvaluation(value = ScientificParser(expression, mode).evaluate())
} catch (error: ScientificFailure) { ScientificEvaluation(error = error.reason) }

private class ScientificParser(private val input: String, private val mode: ScientificAngleMode) {
    private var position = 0
    private var tokens = 0
    private var depth = 0
    private fun fail(reason: ScientificError = ScientificError.SYNTAX): Nothing = throw ScientificFailure(reason)
    private fun spaces() { while (position < input.length && input[position].isWhitespace()) position++ }
    private fun token() { if (++tokens > 512) fail(ScientificError.LIMIT) }
    private fun take(char: Char): Boolean {
        spaces()
        if (position < input.length && input[position] == char) { position++; token(); return true }
        return false
    }
    private fun finite(value: Double): Double = value.also { if (!it.isFinite()) fail(ScientificError.NON_FINITE) }
    private fun nested(action: () -> Double): Double {
        if (++depth > 32) fail(ScientificError.LIMIT)
        return try { action() } finally { depth-- }
    }
    fun evaluate(): Double {
        val value = sum()
        spaces()
        if (position != input.length) fail()
        return finite(value)
    }
    private fun sum(): Double {
        var value = product()
        while (true) value = when {
            take('+') -> finite(value + product())
            take('-') -> finite(value - product())
            else -> return value
        }
    }
    private fun product(): Double {
        var value = unary()
        while (true) value = when {
            take('*') -> finite(value * unary())
            take('/') -> { val divisor = unary(); if (divisor == 0.0) fail(ScientificError.DIVISION_BY_ZERO); finite(value / divisor) }
            else -> return value
        }
    }
    // Unary is outside power on the left; power's right operand accepts unary: -2^2=-4, 2^-2=.25.
    private fun unary(): Double = when {
        take('+') -> nested { unary() }
        take('-') -> -nested { unary() }
        else -> power()
    }
    private fun power(): Double {
        val base = primary()
        if (!take('^')) return base
        val exponent = nested { unary() }
        if (base == 0.0 && exponent < 0.0) fail(ScientificError.DIVISION_BY_ZERO)
        if (base == 0.0 && exponent == 0.0) fail(ScientificError.DOMAIN)
        if (base < 0 && exponent != floor(exponent)) fail(ScientificError.DOMAIN)
        return finite(base.pow(exponent))
    }
    private fun primary(): Double {
        spaces()
        if (take('(')) {
            val value = nested { sum() }
            if (!take(')')) fail()
            return value
        }
        if (position >= input.length) fail()
        if (input[position].isDigit() || input[position] == '.') return number()
        if (input[position] in 'a'..'z') {
            val start = position
            while (position < input.length && input[position] in 'a'..'z') position++
            token()
            val name = input.substring(start, position)
            if (name == "pi") return PI
            if (name == "e") return E
            if (name !in setOf("sin", "cos", "tan", "asin", "acos", "atan", "sqrt", "ln", "log", "abs", "exp")) fail()
            if (!take('(')) fail()
            val value = nested { sum() }
            if (!take(')')) fail()
            return function(name, value)
        }
        fail()
    }
    private fun number(): Double {
        val start = position
        var digits = 0
        while (position < input.length && input[position] in '0'..'9') { position++; digits++ }
        if (position < input.length && input[position] == '.') {
            position++
            while (position < input.length && input[position] in '0'..'9') { position++; digits++ }
        }
        if (digits == 0) fail()
        if (position < input.length && input[position] in "eE") {
            position++
            if (position < input.length && input[position] in "+-") position++
            val exponentStart = position
            while (position < input.length && input[position] in '0'..'9') position++
            if (position == exponentStart) fail()
        }
        token()
        return finite(input.substring(start, position).toDoubleOrNull() ?: fail())
    }
    private fun function(name: String, value: Double): Double {
        fun angle() = if (mode == ScientificAngleMode.DEG) value / 180.0 * PI else value
        fun inverse(radians: Double) = if (mode == ScientificAngleMode.DEG) radians / PI * 180.0 else radians
        return finite(when (name) {
            "sin" -> sin(angle())
            "cos" -> cos(angle())
            "tan" -> { val radians = angle(); if (abs(cos(radians)) < 1e-14) fail(ScientificError.DOMAIN); tan(radians) }
            "asin" -> { if (value !in -1.0..1.0) fail(ScientificError.DOMAIN); inverse(asin(value)) }
            "acos" -> { if (value !in -1.0..1.0) fail(ScientificError.DOMAIN); inverse(acos(value)) }
            "atan" -> inverse(atan(value))
            "sqrt" -> { if (value < 0) fail(ScientificError.DOMAIN); sqrt(value) }
            "ln" -> { if (value <= 0) fail(ScientificError.DOMAIN); ln(value) }
            "log" -> { if (value <= 0) fail(ScientificError.DOMAIN); log10(value) }
            "abs" -> abs(value)
            "exp" -> exp(value)
            else -> fail()
        })
    }
}
