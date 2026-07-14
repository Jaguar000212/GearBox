package com.jaguar.gearbox.ui.components

import androidx.compose.runtime.saveable.Saver

/**
 * Shared `stateSaver`s for `rememberSaveable(stateSaver = ..., init = { mutableStateOf(...) })`.
 * Bundle can't reliably save a bare `List<T>` (Kotlin doesn't guarantee its backing type is
 * `Serializable`), so these round-trip through a primitive array instead - the same fix that used
 * to be hand-rolled per screen as `Saver<MutableState<List<T>>, TArray>` with its own
 * `mutableStateOf(it.toList())` restore lambda. Using `stateSaver` (rather than `saver`) lets
 * `rememberSaveable` handle the `MutableState` wrapping itself, so this only needs to describe
 * the plain value type.
 */
val IntListSaver: Saver<List<Int>, IntArray> = Saver(
    save = { it.toIntArray() },
    restore = { it.toList() },
)

val LongListSaver: Saver<List<Long>, LongArray> = Saver(
    save = { it.toLongArray() },
    restore = { it.toList() },
)

val StringListSaver: Saver<List<String>, Array<String>> = Saver(
    save = { it.toTypedArray() },
    restore = { it.toList() },
)
