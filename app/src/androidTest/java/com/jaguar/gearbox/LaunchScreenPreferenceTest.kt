package com.jaguar.gearbox

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.LaunchScreen
import com.jaguar.gearbox.data.LaunchScreenStore
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class LaunchScreenPreferenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val name = "launch_test_${UUID.randomUUID()}"
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    @After fun cleanup() { context.deleteSharedPreferences(name) }

    @Test fun savesAndReloadsWithoutTouchingOtherPreferences() {
        prefs.edit().putString("sentinel", "keep").commit()
        val store = LaunchScreenStore(prefs)
        assertEquals(LaunchScreen.HOME, store.load())
        store.save(LaunchScreen.FAVORITES)
        assertEquals(LaunchScreen.FAVORITES, LaunchScreenStore(prefs).load())
        store.save(LaunchScreen.HOME)
        assertEquals(LaunchScreen.HOME, LaunchScreenStore(prefs).load())
        assertEquals("keep", prefs.getString("sentinel", null))
    }
    @Test fun invalidStoredTypeFallsBackWithoutClearingData() {
        prefs.edit().putInt("settings.launch_screen", 7).commit()
        assertEquals(LaunchScreen.HOME, LaunchScreenStore(prefs).load())
        assertEquals(7, prefs.getInt("settings.launch_screen", 0))
    }
}
