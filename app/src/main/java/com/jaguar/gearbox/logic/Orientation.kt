package com.jaguar.gearbox.logic

import kotlin.math.abs

/** Device orientation angles in radians, as returned by `SensorManager.getOrientation`. */
data class OrientationSnapshot(val azimuthRad: Float, val pitchRad: Float, val rollRad: Float)

private const val FILTER_ALPHA = 0.15f
private const val DEADBAND_DEGREES = 0.1

/**
 * Exponential low-pass smoothing with shortest-path wraparound for azimuth (so heading doesn't
 * jump +/-360 degrees crossing north), plus a deadband so callers can skip near-identical updates.
 * Constants (alpha=0.15, deadband=0.1 degrees) carried over from a previous project's sensor
 * tuning rather than re-derived from scratch.
 *
 * Returns null if the change since [previous] is within the deadband - i.e. nothing worth
 * re-rendering - so the caller should keep displaying [previous] in that case.
 */
fun smoothOrientation(previous: OrientationSnapshot, raw: OrientationSnapshot): OrientationSnapshot? {
    var diffAzimuth = raw.azimuthRad - previous.azimuthRad
    if (diffAzimuth > Math.PI) diffAzimuth -= (2 * Math.PI).toFloat()
    if (diffAzimuth < -Math.PI) diffAzimuth += (2 * Math.PI).toFloat()
    val nextAzimuth = previous.azimuthRad + FILTER_ALPHA * diffAzimuth
    val nextPitch = previous.pitchRad + FILTER_ALPHA * (raw.pitchRad - previous.pitchRad)
    val nextRoll = previous.rollRad + FILTER_ALPHA * (raw.rollRad - previous.rollRad)

    val deltaA = abs(Math.toDegrees((nextAzimuth - previous.azimuthRad).toDouble()))
    val deltaP = abs(Math.toDegrees((nextPitch - previous.pitchRad).toDouble()))
    val deltaR = abs(Math.toDegrees((nextRoll - previous.rollRad).toDouble()))
    if (deltaA <= DEADBAND_DEGREES && deltaP <= DEADBAND_DEGREES && deltaR <= DEADBAND_DEGREES) return null

    return OrientationSnapshot(nextAzimuth, nextPitch, nextRoll)
}

/** Azimuth in degrees, normalized to 0..360 (0 = north). */
fun azimuthDegrees(azimuthRad: Float): Double = (Math.toDegrees(azimuthRad.toDouble()) + 360) % 360

private val COMPASS_POINTS = listOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
)

/** Nearest 16-point compass direction (N, NNE, NE, ...) for a 0..360 heading. */
fun compassPoint(degrees: Double): String {
    val index = (Math.floor(degrees / 22.5 + 0.5).toInt()) % 16
    return COMPASS_POINTS[if (index < 0) index + 16 else index]
}
