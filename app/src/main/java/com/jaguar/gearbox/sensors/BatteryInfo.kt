package com.jaguar.gearbox.sensors

import kotlin.math.roundToInt

enum class BatteryStatus { UNKNOWN, CHARGING, DISCHARGING, NOT_CHARGING, FULL }
enum class BatteryPower { UNKNOWN, BATTERY, AC, USB, WIRELESS, DOCK }
enum class BatteryHealth { UNKNOWN, GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, FAILURE, COLD }
enum class BatteryInfoStatus { STOPPED, WAITING, AVAILABLE, UNAVAILABLE }
data class BatterySnapshot(
    val present: Boolean? = null,
    val percent: Int? = null,
    val temperatureCelsius: Double? = null,
    val voltageVolts: Double? = null,
    val currentMilliamps: Double? = null,
    val chargeMilliampHours: Double? = null,
    val status: BatteryStatus = BatteryStatus.UNKNOWN,
    val power: BatteryPower = BatteryPower.UNKNOWN,
    val health: BatteryHealth = BatteryHealth.UNKNOWN,
)
data class BatteryInfoState(val status: BatteryInfoStatus = BatteryInfoStatus.STOPPED, val snapshot: BatterySnapshot? = null)
fun batteryPercent(level: Int?, scale: Int?): Int? =
    if (level != null && scale != null && scale > 0 && level in 0..scale)
        (level.toDouble() / scale * 100).roundToInt() else null
fun batteryCurrentMilliamps(raw: Int?): Double? = raw?.takeUnless { it == Int.MIN_VALUE || it == Int.MAX_VALUE }?.div(1000.0)
fun batteryChargeMilliampHours(raw: Int?): Double? = raw?.takeIf { it >= 0 && it != Int.MAX_VALUE }?.div(1000.0)
fun sanitizeBatteryPresence(value: BatterySnapshot): BatterySnapshot =
    if (value.present == false) BatterySnapshot(present = false) else value
interface BatteryInfoSource {
    fun start(onSnapshot: (BatterySnapshot) -> Unit): Boolean
    fun stop()
}
/** Source callbacks and lifecycle commands run on the main thread. */
class BatteryInfoController(private val source: BatteryInfoSource, private val onState: (BatteryInfoState) -> Unit) {
    private var epoch = 0L
    private var active = false
    fun start() {
        if (active) return
        active = true
        val generation = ++epoch
        onState(BatteryInfoState(BatteryInfoStatus.WAITING))
        try {
            check(source.start { value ->
                if (active && epoch == generation) onState(BatteryInfoState(BatteryInfoStatus.AVAILABLE, sanitizeBatteryPresence(value)))
            })
        } catch (_: Exception) {
            active = false
            ++epoch
            runCatching { source.stop() }
            onState(BatteryInfoState(BatteryInfoStatus.UNAVAILABLE))
        }
    }
    fun stop() {
        active = false
        ++epoch
        runCatching { source.stop() }
        onState(BatteryInfoState())
    }
}
