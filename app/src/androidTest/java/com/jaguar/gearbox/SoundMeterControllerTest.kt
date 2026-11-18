package com.jaguar.gearbox

import com.jaguar.gearbox.sensors.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class SoundMeterControllerTest {
    @Test fun stopClearsStateAndWorkerClosesAndZeroesBuffer() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val closes = AtomicInteger()
        var observed: ShortArray? = null
        val state = java.util.concurrent.atomic.AtomicReference(SoundMeterState())
        val source = object : SoundMeterSource {
            override fun start() {}
            override fun read(buffer:ShortArray, offset:Int, count:Int):Int {
                observed = buffer
                for (i in offset until offset+count) buffer[i] = 1000
                return count
            }
            override fun close() { closes.incrementAndGet() }
        }
        val controller = SoundMeterController(scope, { source }, state::set)
        try {
            withContext(Dispatchers.Main) { controller.start(true, true) }
            withTimeout(3000) { while (state.get().status != SoundMeterStatus.READING) delay(10) }
            withContext(Dispatchers.Main) { controller.stop() }
            withTimeout(3000) { while (closes.get() == 0) delay(10) }
            assertEquals(SoundMeterStatus.STOPPED, state.get().status)
            assertTrue(observed!!.all { it == 0.toShort() })
        } finally { withContext(Dispatchers.Main) { controller.stop() }; scope.cancel() }
    }
    @Test fun competingControllerReportsBusyWithoutOpeningSecondSource() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val started = AtomicInteger()
        val closed = AtomicInteger()
        val source = object : SoundMeterSource {
            override fun start() { started.incrementAndGet() }
            override fun read(buffer: ShortArray, offset: Int, count: Int) = 0
            override fun close() { closed.incrementAndGet() }
        }
        val first = SoundMeterController(scope, { source }, {})
        val secondState = java.util.concurrent.atomic.AtomicReference(SoundMeterState())
        val second = SoundMeterController(scope, { error("Busy source must not be opened") }, secondState::set)
        try {
            withContext(Dispatchers.Main) { first.start(true, true) }
            withTimeout(3000) { while (started.get() == 0) delay(10) }
            withContext(Dispatchers.Main) { second.start(true, true) }
            withTimeout(3000) { while (secondState.get().status != SoundMeterStatus.BUSY) delay(10) }
            withContext(Dispatchers.Main) { first.stop() }
            withTimeout(3000) { while (closed.get() == 0) delay(10) }
        } finally { withContext(Dispatchers.Main) { first.stop(); second.stop() }; scope.cancel() }
    }
    @Test fun permissionAndBackgroundNeverStartSource() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        var starts = 0
        val controller = SoundMeterController(scope, { starts++; error("must not construct") }, {})
        withContext(Dispatchers.Main) {
            controller.start(false, true)
            controller.start(true, false)
            controller.stop()
        }
        delay(50)
        assertEquals(0, starts)
        scope.cancel()
    }
}
