package com.jaguar.gearbox.sensors

enum class LightMeterStatus { STOPPED, UNAVAILABLE, REGISTRATION_FAILED, WAITING, READING }
enum class LightMeterAccuracy { UNKNOWN, UNRELIABLE, LOW, MEDIUM, HIGH }
data class LightMeterState(
    val status: LightMeterStatus = LightMeterStatus.STOPPED,
    val lux: Float? = null,
    val accuracy: LightMeterAccuracy = LightMeterAccuracy.UNKNOWN,
    val maximumRange: Float? = null,
    val resolution: Float? = null,
)
fun validLightLux(value: Float): Boolean = value.isFinite() && value >= 0f
fun lightMeterAccuracy(value: Int): LightMeterAccuracy = when(value) {
    0 -> LightMeterAccuracy.UNRELIABLE
    1 -> LightMeterAccuracy.LOW
    2 -> LightMeterAccuracy.MEDIUM
    3 -> LightMeterAccuracy.HIGH
    else -> LightMeterAccuracy.UNKNOWN
}

/** Foreground-only source. Implementations deliver callbacks on the UI thread. */
interface LightMeterSource {
    val available: Boolean
    val maximumRange: Float?
    val resolution: Float?
    fun start(onReading: (Float, Int) -> Unit, onAccuracy: (Int) -> Unit): Boolean
    fun stop()
}

/** UI-thread controller with epoch fencing: a queued callback from an earlier listener cannot
 * revive a stopped reading, including after a later start. No history, smoothing or calibration. */
class LightMeterController(private val source: LightMeterSource, private val onState: (LightMeterState) -> Unit) {
    private var epoch = 0L
    private var started = false
    private var attempted = false
    private var state = LightMeterState()
    private fun publish(next: LightMeterState) { state=next; onState(next) }
    private fun base(status: LightMeterStatus) = LightMeterState(status=status,
        maximumRange=source.maximumRange?.takeIf { it.isFinite() && it > 0f },
        resolution=source.resolution?.takeIf { it.isFinite() && it > 0f })
    fun start() {
        if (started) return
        started=true
        val token=++epoch
        if (!source.available) { publish(base(LightMeterStatus.UNAVAILABLE)); return }
        publish(base(LightMeterStatus.WAITING))
        attempted=true
        val registered=try { source.start({ value, accuracy ->
            if (started && token == epoch) {
                // An invalid current sample must not leave a previous value presented as current.
                publish(state.copy(status=if(validLightLux(value)) LightMeterStatus.READING else LightMeterStatus.WAITING,
                    lux=value.takeIf(::validLightLux), accuracy=lightMeterAccuracy(accuracy)))
            }
        }, { accuracy ->
            if (started && token == epoch) publish(state.copy(accuracy=lightMeterAccuracy(accuracy)))
        }) } catch (_: RuntimeException) { false }
        if (!registered) {
            ++epoch
            try { source.stop() } catch (_: RuntimeException) { }
            attempted=false
            publish(base(LightMeterStatus.REGISTRATION_FAILED))
        }
    }
    fun stop() {
        ++epoch; started=false
        if (attempted) { attempted=false; try { source.stop() } catch (_: RuntimeException) { } }
        publish(base(LightMeterStatus.STOPPED))
    }
}
