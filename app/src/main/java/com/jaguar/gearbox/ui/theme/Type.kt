package com.jaguar.gearbox.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// A locally available, legible type system. No downloaded font is needed for first launch.
private val baseline = Typography()
val AppTypography = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = FontFamily.SansSerif),
    displayMedium = baseline.displayMedium.copy(fontFamily = FontFamily.SansSerif),
    displaySmall = baseline.displaySmall.copy(fontFamily = FontFamily.SansSerif),
    headlineLarge = baseline.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = baseline.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = baseline.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = baseline.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = baseline.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = baseline.titleSmall.copy(fontWeight = FontWeight.Medium),
    bodyLarge = baseline.bodyLarge,
    bodyMedium = baseline.bodyMedium,
    bodySmall = baseline.bodySmall,
    labelLarge = baseline.labelLarge,
    labelMedium = baseline.labelMedium,
    labelSmall = baseline.labelSmall,
)
