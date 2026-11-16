package com.jaguar.gearbox

import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.CounterStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CounterRestoreTest {
    @Test fun restoreClearsLimitAndWorksAcrossStoreInstances() {
        // Instrumentation package storage, never the user's application data.
        val context = InstrumentationRegistry.getInstrumentation().context
        val first = CounterStore(context)
        val second = CounterStore(context)
        val original = first.count
        try {
            first.restore(Int.MAX_VALUE)
            first.adjust(1)
            second.restore(-42)
            assertEquals(-42, first.count)
            assertFalse(first.limitReached)
            first.adjust(1)
            assertEquals(-41, second.count)
        } finally {
            first.restore(original)
        }
    }
}
