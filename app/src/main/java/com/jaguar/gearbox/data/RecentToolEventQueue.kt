package com.jaguar.gearbox.data

import kotlinx.coroutines.channels.Channel

/** UI events enter synchronously; one consumer preserves their order even when storage uses IO. */
class RecentToolEventQueue(private val record: suspend (String, Long?) -> Unit) {
    private data class Event(val route: String, val generation: Long?)
    private val events = Channel<Event>(Channel.UNLIMITED)
    fun enqueue(route: String, generation: Long? = null): Boolean = events.trySend(Event(route, generation)).isSuccess
    suspend fun consume() {
        for (event in events) record(event.route, event.generation)
    }
}
