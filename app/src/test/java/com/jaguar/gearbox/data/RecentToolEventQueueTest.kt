package com.jaguar.gearbox.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class RecentToolEventQueueTest {
    @Test fun aSlowFirstWriteCannotBeOvertakenByTheNextTool() = runBlocking {
        val firstStarted = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()
        val seen = mutableListOf<String>()
        val queue = RecentToolEventQueue { route, generation ->
            assertEquals(17L, generation)
            if (route == "tool/tip") { firstStarted.complete(Unit); releaseFirst.await() }
            seen += route
            if (route == "tool/percentage") finished.complete(Unit)
        }
        assertTrue(queue.enqueue("tool/tip", 17L))
        assertTrue(queue.enqueue("tool/percentage", 17L))
        val consumer = launch { queue.consume() }
        try {
            withTimeout(5000) { firstStarted.await() }
            assertTrue(seen.isEmpty())
            releaseFirst.complete(Unit)
            withTimeout(5000) { finished.await() }
            assertEquals(listOf("tool/tip", "tool/percentage"), seen)
        } finally { consumer.cancelAndJoin() }
    }
}
