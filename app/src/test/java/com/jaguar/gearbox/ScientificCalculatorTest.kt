package com.jaguar.gearbox

import com.jaguar.gearbox.logic.*
import org.junit.Assert.*
import org.junit.Test

class ScientificCalculatorTest {
    private fun value(expression: String, mode: ScientificAngleMode = ScientificAngleMode.DEG) =
        requireNotNull(evaluateScientific(expression, mode).value)
    private fun error(expression: String) = evaluateScientific(expression, ScientificAngleMode.DEG).error
    @Test fun arithmeticPowerAndUnaryPrecedence() {
        assertEquals(14.0, value("2+3*4"), 0.0)
        assertEquals(512.0, value("2^3^2"), 0.0)
        assertEquals(-4.0, value("-2^2"), 0.0)
        assertEquals(4.0, value("(-2)^2"), 0.0)
        assertEquals(0.25, value("2^-2"), 0.0)
        assertEquals(4.0, value("8/2/2+2"), 0.0)
    }
    @Test fun functionsAndAngleModes() {
        assertEquals(0.5, value("sin(30)"), 1e-14)
        assertEquals(1.0, value("sin(pi/2)", ScientificAngleMode.RAD), 1e-14)
        assertEquals(30.0, value("asin(0.5)"), 1e-12)
        assertEquals(Math.PI / 2, value("acos(0)", ScientificAngleMode.RAD), 1e-14)
        assertEquals(45.0, value("atan(1)"), 1e-12)
        assertEquals(9.0, value("sqrt(9)+ln(e)+log(100)+abs(-2)+exp(0)"), 1e-14)
    }
    @Test fun scientificNotationAndExplicitEConstant() {
        assertEquals(1500.0002, value("1.5e3+2E-4"), 1e-12)
        assertEquals(2 * Math.E, value("2*e"), 1e-14)
        assertEquals(ScientificError.SYNTAX, error("2e"))
        assertEquals(ScientificError.SYNTAX, error("2pi"))
        assertEquals(ScientificError.SYNTAX, error("2(3)"))
    }
    @Test fun domainsAndNonfiniteFailWithoutRawExceptions() {
        for (expression in listOf("sqrt(-1)", "ln(0)", "log(-1)", "asin(2)", "acos(-2)", "(-2)^0.5", "0^0", "tan(90)"))
            assertEquals(expression, ScientificError.DOMAIN, error(expression))
        for (expression in listOf("1/0", "1/(-0)", "0^-1")) assertEquals(ScientificError.DIVISION_BY_ZERO, error(expression))
        for (expression in listOf("1e309", "exp(10000)", "1e308*10")) assertEquals(ScientificError.NON_FINITE, error(expression))
    }
    @Test fun maliciousAndMalformedInputNeverExecutes() {
        for (expression in listOf("", "System.exit(0)", "Runtime.exec(1)", "1;2", "__import__('os')", "[1]", "NaN", "Infinity", "sin 30", "2**3", "1,000", "foo(1)", "(1+2", "."))
            assertEquals(expression, ScientificError.SYNTAX, error(expression))
    }
    @Test fun lengthAndRecursionAreBounded() {
        assertEquals(ScientificError.LIMIT, error("1".repeat(513)))
        assertEquals(ScientificError.LIMIT, error("(".repeat(33) + "1" + ")".repeat(33)))
        assertEquals(ScientificError.LIMIT, error("-".repeat(33) + "1"))
        assertEquals(ScientificError.LIMIT, error("2^".repeat(33) + "1"))
        assertEquals(1.0, value("(".repeat(32) + "1" + ")".repeat(32)), 0.0)
    }
}
