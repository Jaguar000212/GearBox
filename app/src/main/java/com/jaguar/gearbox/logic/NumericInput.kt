package com.jaguar.gearbox.logic

/** Blank input stays neutral; tool-specific required/range rules remain with the caller. */
fun isInvalidNumericInput(value: String): Boolean = value.isNotBlank() &&
    value.trim().toDoubleOrNull()?.isFinite() != true
