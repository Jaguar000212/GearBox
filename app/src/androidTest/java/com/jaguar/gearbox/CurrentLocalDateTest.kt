package com.jaguar.gearbox

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jaguar.gearbox.ui.components.rememberCurrentLocalDate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test

class CurrentLocalDateTest {
    @get:Rule val rule = createComposeRule()

    @Test fun resumeRefreshesDateAndTimezoneWithoutInputChanges() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle get() = registry
        }
        var clock = Clock.fixed(Instant.parse("2026-11-14T12:00:00Z"), ZoneId.of("Asia/Kolkata"))
        rule.runOnUiThread { owner.registry.currentState = Lifecycle.State.RESUMED }
        rule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                val today by rememberCurrentLocalDate { clock }
                Text(today.toString())
            }
        }
        rule.onNodeWithText("2026-11-14").assertExists()
        rule.runOnIdle {
            owner.registry.currentState = Lifecycle.State.CREATED
            clock = Clock.offset(clock, Duration.ofHours(7))
        }
        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        rule.waitForIdle()
        rule.onNodeWithText("2026-11-15").assertExists()
        rule.runOnIdle {
            owner.registry.currentState = Lifecycle.State.CREATED
            clock = clock.withZone(ZoneId.of("America/Los_Angeles"))
        }
        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        rule.waitForIdle()
        rule.onNodeWithText("2026-11-14").assertExists()
        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.DESTROYED }
    }
}
