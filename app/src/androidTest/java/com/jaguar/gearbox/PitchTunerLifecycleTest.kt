package com.jaguar.gearbox

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaguar.gearbox.sensors.*
import com.jaguar.gearbox.ui.components.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class PitchTunerLifecycleTest {
    @get:Rule val rule = createComposeRule()
    @Test fun startsOnlyExplicitlyAndNeverResumesAfterBackground() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle get() = registry
        }
        val starts = AtomicInteger()
        val closes = AtomicInteger()
        val source = object : SoundMeterSource {
            override fun start() { starts.incrementAndGet() }
            override fun read(buffer: ShortArray, offset: Int, count: Int) = 0
            override fun close() { closes.incrementAndGet() }
        }
        var visible by mutableStateOf(true)
        lateinit var handle: PitchTunerHandle
        rule.runOnUiThread { owner.registry.currentState = Lifecycle.State.STARTED }
        rule.setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) {
            if (visible) {
                handle = rememberPitchTunerWithPermission({ true }, { source })
                Text(handle.state.status.name)
            }
        } }
        rule.runOnIdle { assertEquals(0, starts.get()); handle.start() }
        rule.waitUntil(3000) { starts.get() == 1 }
        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        rule.waitUntil(3000) { closes.get() == 1 }
        rule.runOnIdle { assertEquals(SoundMeterStatus.STOPPED, handle.state.status); owner.registry.currentState = Lifecycle.State.STARTED }
        rule.runOnIdle { assertEquals(1, starts.get()); handle.start() }
        rule.waitUntil(3000) { starts.get() == 2 }
        rule.runOnIdle { visible = false }
        rule.waitUntil(3000) { closes.get() == 2 }
    }
}
