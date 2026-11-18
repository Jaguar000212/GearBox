package com.jaguar.gearbox.sensors

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.*

const val SOUND_WINDOW_SAMPLES = 2048
data class SoundLevels(val rmsDbfs: Double, val peakDbfs: Double)
fun soundLevels(samples: ShortArray): SoundLevels {
    require(samples.size == SOUND_WINDOW_SAMPLES)
    var squares = 0.0
    var peak = 0.0
    for (sample in samples) {
        val value = abs(sample.toDouble()) / 32768.0
        squares += value * value
        peak = max(peak, value)
    }
    fun db(value: Double) = if (value == 0.0) Double.NEGATIVE_INFINITY else 20 * log10(value)
    return SoundLevels(db(sqrt(squares / samples.size)), db(peak))
}
enum class SoundMeterStatus { STOPPED, STARTING, READING, BUSY, ERROR, PERMISSION_REQUIRED }
data class SoundMeterState(val status: SoundMeterStatus = SoundMeterStatus.STOPPED, val levels: SoundLevels? = null)
interface SoundMeterSource {
    fun start()
    /** Nonblocking read; negative values indicate failure. */
    fun read(buffer: ShortArray, offset: Int, count: Int): Int
    fun close()
}
internal val soundCaptureMutex = Mutex()
class AndroidSoundMeterSource : SoundMeterSource {
    private var recorder: AudioRecord? = null
    override fun start() {
        try {
            val minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(minimum > 0)
            val audio = AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, max(minimum, SOUND_WINDOW_SAMPLES * 2))
            recorder = audio
            check(audio.state == AudioRecord.STATE_INITIALIZED)
            audio.startRecording()
            check(audio.recordingState == AudioRecord.RECORDSTATE_RECORDING)
        } catch (denied: SecurityException) {
            // Permission can be revoked between the UI check and native construction/start.
            // Release any partially initialized recorder, preserving the permission failure.
            try { close() } catch (_: Exception) { }
            throw denied
        }
    }
    override fun read(buffer: ShortArray, offset: Int, count: Int) =
        recorder?.read(buffer, offset, count, AudioRecord.READ_NON_BLOCKING) ?: -1
    override fun close() {
        val audio = recorder
        recorder = null
        try { audio?.stop() } finally { audio?.release() }
    }
}
/** Commands are main-thread only. One IO worker owns start/read/close; stop never releases under read. */
class SoundMeterController(
    private val scope: CoroutineScope,
    private val sourceFactory: () -> SoundMeterSource = { AndroidSoundMeterSource() },
    private val onState: (SoundMeterState) -> Unit,
) {
    private val epoch = AtomicLong()
    private var job: Job? = null
    fun start(foreground: Boolean, permitted: Boolean) {
        if (!foreground) return
        if (!permitted) { stop(); onState(SoundMeterState(SoundMeterStatus.PERMISSION_REQUIRED)); return }
        if (job?.isActive == true) return
        val generation = epoch.incrementAndGet()
        onState(SoundMeterState(SoundMeterStatus.STARTING))
        job = scope.launch(Dispatchers.IO) {
            suspend fun publish(value: SoundMeterState) = withContext(Dispatchers.Main.immediate) {
                if (epoch.get() == generation) onState(value)
            }
            if (!soundCaptureMutex.tryLock()) { publish(SoundMeterState(SoundMeterStatus.BUSY)); return@launch }
            val buffer = ShortArray(SOUND_WINDOW_SAMPLES)
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
                        publish(SoundMeterState(SoundMeterStatus.READING, soundLevels(buffer)))
                        buffer.fill(0)
                        filled = 0
                        delay(20)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: SecurityException) { publish(SoundMeterState(SoundMeterStatus.PERMISSION_REQUIRED)) }
            catch (_: Exception) { publish(SoundMeterState(SoundMeterStatus.ERROR)) }
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
        onState(SoundMeterState())
    }
}
