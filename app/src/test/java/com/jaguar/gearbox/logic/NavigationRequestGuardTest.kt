package com.jaguar.gearbox.logic

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class NavigationRequestGuardTest {
    @Test fun newerIntentAndManualNavigationInvalidateOlderRequests() {
        val guard = NavigationRequestGuard()
        val first = guard.begin()
        assertTrue(guard.isCurrent(first))
        val second = guard.begin()
        assertFalse(guard.isCurrent(first))
        assertTrue(guard.isCurrent(second))
        guard.invalidate()
        assertFalse(guard.isCurrent(second))
    }

    @Test fun manualNavigationWhileAnIntentAwaitsIoPreventsLateNavigation() = runBlocking {
        val guard = NavigationRequestGuard()
        val request = guard.begin()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var navigated = false
        val pending = launch {
            started.complete(Unit)
            release.await()
            if (guard.isCurrent(request)) navigated = true
        }
        withTimeout(5000) { started.await() }
        guard.invalidate()
        release.complete(Unit)
        withTimeout(5000) { pending.join() }
        assertFalse(navigated)
    }
}
