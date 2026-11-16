package com.jaguar.gearbox.data

import kotlinx.coroutines.channels.Channel

/** UI events enter synchronously; one consumer preserves their order even when storage uses IO. */
class RecentToolEventQueue(private val record: suspend (String) -> Unit) {
    private val events = Channel<String>(Channel.UNLIMITED)
    fun enqueue(route: String): Boolean = events.trySend(route).isSuccess
    suspend fun consume() {
        for (route in events) record(route)
    }
}
