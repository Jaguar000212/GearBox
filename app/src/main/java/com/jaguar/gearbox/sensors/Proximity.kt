package com.jaguar.gearbox.sensors

enum class ProximityStatus { STOPPED, UNAVAILABLE, REGISTRATION_FAILED, WAITING, READING }
enum class ProximityAccuracy { UNKNOWN, UNRELIABLE, LOW, MEDIUM, HIGH }
data class ProximityState(
    val status: ProximityStatus = ProximityStatus.STOPPED,
    val distanceCm: Float? = null,
    val accuracy: ProximityAccuracy = ProximityAccuracy.UNKNOWN,
    val maximumRange: Float? = null,
    val resolution: Float? = null,
    val classification: ProximityClassification = ProximityClassification.UNKNOWN,
)
fun validProximityDistance(value: Float): Boolean = value.isFinite() && value >= 0f
fun proximityAccuracy(value: Int): ProximityAccuracy = when(value) {
    0 -> ProximityAccuracy.UNRELIABLE
    1 -> ProximityAccuracy.LOW
    2 -> ProximityAccuracy.MEDIUM
    3 -> ProximityAccuracy.HIGH
    else -> ProximityAccuracy.UNKNOWN
}

/** Foreground-only source. Implementations deliver callbacks on the UI thread. */
interface ProximitySource {
    val available: Boolean
    val maximumRange: Float?
    val resolution: Float?
    fun start(onReading: (Float, Int) -> Unit, onAccuracy: (Int) -> Unit): Boolean
    fun stop()
}

/** UI-thread controller with epoch fencing: a queued callback from an earlier listener cannot
 * revive a stopped reading, including after a later start. No history, smoothing or calibration. */
class ProximityController(private val source: ProximitySource, private val onState: (ProximityState) -> Unit) {
    private var epoch = 0L
    private var started = false
    private var attempted = false
    private var state = ProximityState()
    private fun publish(next: ProximityState) { state=next; onState(next) }
    private fun base(status: ProximityStatus) = ProximityState(status=status,
        maximumRange=source.maximumRange?.takeIf { it.isFinite() && it > 0f },
        resolution=source.resolution?.takeIf { it.isFinite() && it > 0f })
    fun start() {
        if (started) return
        started=true
        val token=++epoch
        if (!source.available) { publish(base(ProximityStatus.UNAVAILABLE)); return }
        publish(base(ProximityStatus.WAITING))
        attempted=true
        val registered=try { source.start({ value, accuracy ->
            if (started && token == epoch) {
                // An invalid current sample must not leave a previous value presented as current.
                publish(state.copy(status=if(validProximityDistance(value)) ProximityStatus.READING else ProximityStatus.WAITING,
                    distanceCm=value.takeIf(::validProximityDistance), accuracy=proximityAccuracy(accuracy),
                    classification=classifyProximity(value,state.maximumRange)))
            }
        }, { accuracy ->
            if (started && token == epoch) publish(state.copy(accuracy=proximityAccuracy(accuracy)))
        }) } catch (_: RuntimeException) { false }
        if (!registered) {
            ++epoch
            try { source.stop() } catch (_: RuntimeException) { }
            attempted=false
            publish(base(ProximityStatus.REGISTRATION_FAILED))
        }
    }
    fun stop() {
        ++epoch; started=false
        if (attempted) { attempted=false; try { source.stop() } catch (_: RuntimeException) { } }
        publish(base(ProximityStatus.STOPPED))
    }
}

enum class ProximityClassification { UNKNOWN, NEAR, FAR }
fun classifyProximity(value: Float, maximumRange: Float?): ProximityClassification {
    if (!validProximityDistance(value) || maximumRange == null || !maximumRange.isFinite() || maximumRange <= 0f)
        return ProximityClassification.UNKNOWN
    return if (value < maximumRange) ProximityClassification.NEAR else ProximityClassification.FAR
}
