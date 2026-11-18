package com.jaguar.gearbox.logic

import kotlin.math.*
data class DisplayGravity(val x: Double, val y: Double, val z: Double)
enum class AngleFinderAxis { FLAT, SCREEN_Y, SCREEN_X }
fun normalizedDisplayGravity(x: Double, y: Double, z: Double): DisplayGravity? {
    if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null
    val norm = sqrt(x*x+y*y+z*z)
    if (!norm.isFinite() || norm < 1e-9) return null
    return DisplayGravity(x/norm,y/norm,z/norm)
}
fun angleFinderDegrees(gravity: DisplayGravity, axis: AngleFinderAxis): Double? {
    val unit = normalizedDisplayGravity(gravity.x,gravity.y,gravity.z) ?: return null
    return Math.toDegrees(when(axis) {
        AngleFinderAxis.FLAT -> acos(abs(unit.z).coerceIn(0.0,1.0))
        AngleFinderAxis.SCREEN_Y -> asin(unit.y.coerceIn(-1.0,1.0))
        AngleFinderAxis.SCREEN_X -> asin(unit.x.coerceIn(-1.0,1.0))
    })
}
fun shortestAngleDelta(current: Double, baseline: Double): Double? {
    if(!current.isFinite() || !baseline.isFinite()) return null
    val difference = current % 360 - baseline % 360
    return ((difference + 540) % 360 + 360) % 360 - 180
}
