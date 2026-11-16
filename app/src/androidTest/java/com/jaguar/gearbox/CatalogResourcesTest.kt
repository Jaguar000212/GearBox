package com.jaguar.gearbox

import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.logic.*
import java.time.LocalDate
import android.content.res.Configuration
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

/** Resource-backed display must preserve stable routes, input keys and default-English behavior. */
class CatalogResourcesTest {
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val context = base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(Locale.ENGLISH) })

    @Test fun everyCatalogEntryHasMatchingDefaultResourcesWithoutChangingRoutes() {
        val localized = Tools.localized(context)
        assertEquals(Tools.all.map { it.route }, localized.map { it.route })
        Tools.all.zip(localized).forEach { (source, shown) ->
            assertEquals(source.name, shown.name)
            assertEquals(source.description, shown.description)
            assertEquals(source.category.label, source.category.localizedLabel(context))
            assertTrue(shown.keywords.containsAll(source.keywords))
            assertEquals(ToolHelpCatalog.forRoute(source.route), ToolHelpCatalog.localizedForRoute(context, source.route))
        }
    }

    @Test fun examplesAndHandoffsLocalizePresentationWithoutChangingImportPayloads() {
        val today = LocalDate.of(2025, 1, 1)
        ToolExampleCatalog.routes.forEach { route ->
            assertEquals(ToolExampleCatalog.forRoute(route, today), ToolExampleCatalog.localizedForRoute(context, route, today))
        }
        compatibleToolHandoffs(Tools.ROUTE_BASE_CONVERTER, mapOf("input" to "ff", "fromBase" to "HEXADECIMAL"), "Decimal: 255")
            .forEach { source ->
                val shown = source.localized(context)
                assertEquals(source.route, shown.route)
                assertEquals(source.inputs, shown.inputs)
                assertEquals(source.label, shown.label)
                assertEquals(source.detail, shown.detail)
            }
    }

    @Test fun capabilityAndSnapshotResourcesPreserveDefaultMeaning() {
        for (sensor in listOf(true, false, null)) for (flash in listOf(true, false, null)) {
            val value = DeviceCapabilities(sensor, flash)
            Tools.all.forEach { assertEquals(value.hint(it.route), value.localizedHint(context, it.route)) }
        }
        listOf(ClockState(), ClockState(timerRunning = true), ClockState(timerFinished = true),
            ClockState(timerRemainingMillis = 1, stopwatchElapsedMillis = 1),
            ClockState(timerRunning = true, stopwatchRunning = true)).forEach {
            assertEquals(homeClockSummary(it), localizedHomeClockSummary(context, it))
        }
        val tambola = TambolaSnapshot(listOf(2, 10, 90))
        assertEquals(tambola.summary(), tambola.localizedSummary(context))
        val board = TicTacToeSnapshot(List(9) { "" })
        assertEquals(board.summary(), board.localizedSummary(context))
    }
}
