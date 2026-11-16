package com.jaguar.gearbox.logic

import java.util.concurrent.atomic.AtomicLong

/** A deferred shortcut/notification must never navigate over a newer user choice. */
class NavigationRequestGuard {
    private val generation = AtomicLong(0L)
    fun begin(): Long = generation.incrementAndGet()
    fun invalidate() { generation.incrementAndGet() }
    fun isCurrent(request: Long): Boolean = generation.get() == request
}
