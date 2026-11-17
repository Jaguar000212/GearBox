package com.jaguar.gearbox.tiles

import android.content.ContextWrapper
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.MainActivity
import com.jaguar.gearbox.data.EXTRA_SHORTCUT_ROUTE
import org.junit.Assert.*
import org.junit.Test

class ClockTileIntentTest {
    @Test fun launchIntentIsExplicitImmutableTargetDataAndInstallScoped() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        listOf("com.jaguar.gearbox", "com.jaguar.gearbox.beta", "com.jaguar.gearbox.preview", "com.jaguar.gearbox.preview2").forEach { id ->
            val context = object : ContextWrapper(base) { override fun getPackageName() = id }
            ClockTileLaunch.entries.forEach { target ->
                val intent = buildClockTileIntent(context, target)
                assertEquals(id, intent.component?.packageName)
                assertEquals(MainActivity::class.java.name, intent.component?.className)
                assertEquals(Intent.ACTION_VIEW, intent.action)
                assertEquals(target.route, intent.getStringExtra(EXTRA_SHORTCUT_ROUTE))
                assertEquals(setOf(EXTRA_SHORTCUT_ROUTE), intent.extras?.keySet())
                assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK)
                assertNull(intent.data)
                assertNull(intent.clipData)
            }
        }
    }
}
