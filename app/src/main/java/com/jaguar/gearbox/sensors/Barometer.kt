package com.jaguar.gearbox.sensors

enum class BarometerStatus { STOPPED, UNAVAILABLE, REGISTRATION_FAILED, WAITING, READING }
enum class BarometerAccuracy { UNKNOWN, UNRELIABLE, LOW, MEDIUM, HIGH }
data class BarometerState(
    val status: BarometerStatus = BarometerStatus.STOPPED,
    val pressureHpa: Float? = null,
    val accuracy: BarometerAccuracy = BarometerAccuracy.UNKNOWN,
    val maximumRange: Float? = null,
    val resolution: Float? = null,
)
fun validPressureHpa(value: Float): Boolean = value.isFinite() && value in 100f..1200f
fun barometerAccuracy(value: Int): BarometerAccuracy = when(value) {
    0 -> BarometerAccuracy.UNRELIABLE
    1 -> BarometerAccuracy.LOW
    2 -> BarometerAccuracy.MEDIUM
    3 -> BarometerAccuracy.HIGH
    else -> BarometerAccuracy.UNKNOWN
}

/** Foreground-only source. Implementations deliver callbacks on the UI thread. */
interface BarometerSource {
    val available: Boolean
    val maximumRange: Float?
    val resolution: Float?
    fun start(onReading: (Float, Int) -> Unit, onAccuracy: (Int) -> Unit): Boolean
    fun stop()
}

/** UI-thread controller with epoch fencing: a queued callback from an earlier listener cannot
 * revive a stopped reading, including after a later start. No history, smoothing or calibration. */
class BarometerController(private val source: BarometerSource, private val onState: (BarometerState) -> Unit) {
    private var epoch = 0L
    private var started = false
    private var attempted = false
    private var state = BarometerState()
    private fun publish(next: BarometerState) { state=next; onState(next) }
    private fun base(status: BarometerStatus) = BarometerState(status=status,
        maximumRange=source.maximumRange?.takeIf { it.isFinite() && it > 0f },
        resolution=source.resolution?.takeIf { it.isFinite() && it > 0f })
    fun start() {
        if (started) return
        started=true
        val token=++epoch
        if (!source.available) { publish(base(BarometerStatus.UNAVAILABLE)); return }
        publish(base(BarometerStatus.WAITING))
        attempted=true
        val registered=try { source.start({ value, accuracy ->
            if (started && token == epoch) {
                // An invalid current sample must not leave a previous value presented as current.
                publish(state.copy(status=if(validPressureHpa(value)) BarometerStatus.READING else BarometerStatus.WAITING,
                    pressureHpa=value.takeIf(::validPressureHpa), accuracy=barometerAccuracy(accuracy)))
            }
        }, { accuracy ->
            if (started && token == epoch) publish(state.copy(accuracy=barometerAccuracy(accuracy)))
        }) } catch (_: RuntimeException) { false }
        if (!registered) {
            ++epoch
            try { source.stop() } catch (_: RuntimeException) { }
            attempted=false
            publish(base(BarometerStatus.REGISTRATION_FAILED))
        }
    }
    fun stop() {
        ++epoch; started=false
        if (attempted) { attempted=false; try { source.stop() } catch (_: RuntimeException) { } }
        publish(base(BarometerStatus.STOPPED))
    }
}

/** Explicit standard-atmosphere reference, not a fetched or inferred weather observation. */
fun parseSeaLevelReference(value: String): Double? {
    if (!Regex("[1-9][0-9]{2,3}(\\.[0-9]{1,2})?").matches(value)) return null
    return value.toDoubleOrNull()?.takeIf { it in 800.0..1100.0 }
}
fun approximatePressureAltitude(pressureHpa: Double, referenceHpa: Double): Double? {
    if (!pressureHpa.isFinite() || pressureHpa !in 100.0..1200.0 ||
        !referenceHpa.isFinite() || referenceHpa !in 800.0..1100.0) return null
    return 44330.0 * (1.0 - Math.pow(pressureHpa / referenceHpa, 0.1902949))
}
