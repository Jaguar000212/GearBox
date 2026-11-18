package com.jaguar.gearbox.sensors

import kotlin.math.*
const val PITCH_WINDOW_SAMPLES = 4096
data class PitchEstimate(val frequencyHz: Double, val noteNumber: Int, val cents: Double, val confidence: Double)

/** Bounded single-tone periodicity estimate, not polyphonic transcription or calibrated measurement. */
fun detectPitch(samples: ShortArray, checkActive: () -> Unit = {}): PitchEstimate? {
    require(samples.size == PITCH_WINDOW_SAMPLES)
    val mean = samples.sumOf { it.toDouble() } / samples.size
    var energy = 0.0
    var clipped = 0
    for (sample in samples) {
        val centered = sample - mean
        energy += centered * centered
        if (abs(sample.toInt()) >= 32760) clipped++
    }
    if (sqrt(energy / samples.size) < 160 || clipped > samples.size / 100) return null
    val maximumLag = 247
    val size = samples.size - maximumLag
    val difference = DoubleArray(maximumLag + 1)
    for (lag in 1..maximumLag) {
        checkActive()
        var sum = 0.0
        for (i in 0 until size) {
            val delta = samples[i].toDouble() - samples[i + lag]
            sum += delta * delta
        }
        difference[lag] = sum
    }
    var cumulative = 0.0
    val normalized = DoubleArray(maximumLag + 1) { 1.0 }
    for (lag in 1..maximumLag) {
        cumulative += difference[lag]
        normalized[lag] = if (cumulative > 0) difference[lag] * lag / cumulative else 1.0
    }
    // Search short lags too: reject high tones rather than accepting their subharmonics.
    var lag = 2
    while (lag < maximumLag - 1 && normalized[lag] >= 0.12) lag++
    if (lag >= maximumLag - 1) return null
    while (lag < maximumLag - 1 && normalized[lag + 1] < normalized[lag]) lag++
    val confidence = 1.0 - normalized[lag]
    if (confidence < 0.88) return null
    val left = difference[lag - 1]
    val center = difference[lag]
    val right = difference[lag + 1]
    val denominator = left - 2 * center + right
    val offset = if (abs(denominator) > 1e-12) (0.5 * (left - right) / denominator).coerceIn(-0.5, 0.5) else 0.0
    val interpolated = 16000.0 / (lag + offset)
    // Numerical endpoint tolerance only; the reported range is never extended.
    if (!interpolated.isFinite() || interpolated < 65.0 - 0.001 || interpolated > 1000.0 + 0.001) return null
    val frequency = interpolated.coerceIn(65.0, 1000.0)
    val note = 69 + 12 * log2(frequency / 440)
    val nearest = round(note).toInt()
    return PitchEstimate(frequency, nearest, (note - nearest) * 100, confidence.coerceIn(0.0, 1.0))
}
