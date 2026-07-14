package com.jaguar.gearbox.logic

import java.util.Locale

/** Returns the BMI value (formatted) and its WHO weight-status category. */
fun computeBmi(heightM: Double, weightKg: Double): Pair<String, String> {
    val bmi = weightKg / (heightM * heightM)
    val category = when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal weight"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }
    val formatted = String.format(Locale.US, "%.1f", bmi)
    return formatted to category
}
