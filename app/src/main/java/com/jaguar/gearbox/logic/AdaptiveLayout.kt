package com.jaguar.gearbox.logic

/** Shared grid policy: allow readable cards at large fonts and cap very wide grids at four. */
fun adaptiveToolColumnCount(widthDp: Float, fontScale: Float): Int {
    if (!widthDp.isFinite() || widthDp <= 0f) return 1
    val scale = fontScale.takeIf { it.isFinite() && it > 0f }?.coerceAtLeast(1f) ?: 1f
    val availableWidth = (widthDp - 24f).coerceAtLeast(0f)
    val minimumCardWidth = 160f * scale
    return ((availableWidth + 12f) / (minimumCardWidth + 12f)).toInt().coerceIn(1, 4)
}
