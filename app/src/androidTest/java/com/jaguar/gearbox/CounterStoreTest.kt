package com.jaguar.gearbox

import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.CounterStore
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterStoreTest {
    @Test fun screenAndWidgetStoresShareAtomicCount() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = CounterStore(context)
        val second = CounterStore(context)
        first.reset()
        try {
            val workers = List(4) { index ->
                Thread { repeat(100) { (if (index % 2 == 0) first else second).adjust(1) } }
            }
            workers.forEach { it.start() }
            workers.forEach { it.join() }
            assertEquals(400, first.count)
            assertEquals(first.count, second.count)
        } finally {
            first.reset()
        }
    }
}
