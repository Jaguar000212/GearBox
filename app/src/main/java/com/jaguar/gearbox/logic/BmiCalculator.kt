package com.jaguar.gearbox.logic

import java.util.Locale

/** Returns formatted BMI/category, or null for invalid or unrepresentable inputs. */
fun computeBmi(heightM: Double, weightKg: Double): Pair<String, String>? {
    if (!heightM.isFinite() || !weightKg.isFinite() || heightM <= 0 || weightKg <= 0) return null
    val heightSquared = heightM * heightM
    if (!heightSquared.isFinite() || heightSquared <= 0) return null
    val bmi = weightKg / heightSquared
    if (!bmi.isFinite() || bmi <= 0) return null
    val category = when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal weight"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }
    val formatted = String.format(Locale.US, "%.1f", bmi)
    return formatted to category
}
