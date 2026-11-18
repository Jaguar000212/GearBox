package com.jaguar.gearbox.sensors

import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

data class PitchTunerState(val status: SoundMeterStatus = SoundMeterStatus.STOPPED, val estimate: PitchEstimate? = null)

/** Commands are main-thread only. One IO worker owns start/read/close; stop never releases under read. */
class PitchTunerController(
    private val scope: CoroutineScope,
    private val sourceFactory: () -> SoundMeterSource = { AndroidSoundMeterSource() },
    private val onState: (PitchTunerState) -> Unit,
) {
    private val epoch = AtomicLong()
    private var job: Job? = null
    fun start(foreground: Boolean, permitted: Boolean) {
        if (!foreground) return
        if (!permitted) { stop(); onState(PitchTunerState(SoundMeterStatus.PERMISSION_REQUIRED)); return }
        if (job?.isActive == true) return
        val generation = epoch.incrementAndGet()
        onState(PitchTunerState(SoundMeterStatus.STARTING))
        job = scope.launch(Dispatchers.IO) {
            suspend fun publish(value: PitchTunerState) = withContext(Dispatchers.Main.immediate) {
                if (epoch.get() == generation) onState(value)
            }
            if (!soundCaptureMutex.tryLock()) { publish(PitchTunerState(SoundMeterStatus.BUSY)); return@launch }
            val buffer = ShortArray(PITCH_WINDOW_SAMPLES)
            var source: SoundMeterSource? = null
            try {
                ensureActive()
                source = sourceFactory()
                source.start()
                var filled = 0
                var empty = 0
                while (isActive) {
                    val count = source.read(buffer, filled, buffer.size - filled)
                    check(count in 0..(buffer.size - filled))
                    if (count == 0) { check(++empty < 250); delay(20); continue }
                    empty = 0
                    filled += count
                    if (filled == buffer.size) {
                        publish(PitchTunerState(SoundMeterStatus.READING, detectPitch(buffer) { ensureActive() }))
                        buffer.fill(0)
                        filled = 0
                        delay(20)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: SecurityException) { publish(PitchTunerState(SoundMeterStatus.PERMISSION_REQUIRED)) }
            catch (_: Exception) { publish(PitchTunerState(SoundMeterStatus.ERROR)) }
            finally {
                buffer.fill(0)
                try { source?.close() } catch (_: Exception) { /* Cleanup must always unlock. */ }
                soundCaptureMutex.unlock()
            }
        }
    }
    fun stop() {
        epoch.incrementAndGet()
        job?.cancel()
        job = null
        onState(PitchTunerState())
    }
}
