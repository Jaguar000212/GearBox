package com.jaguar.gearbox.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.jaguar.gearbox.logic.calendarDateSnapshot
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest

/** UI-only clock: no alarms or work while this screen is not resumed. */
@Composable
internal fun rememberCurrentLocalDate(clock: () -> Clock = { Clock.systemDefaultZone() }): State<LocalDate> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // A caller can supply a deterministic clock without changing the device clock.
    val currentClock = androidx.compose.runtime.rememberUpdatedState(clock)
    return produceState(calendarDateSnapshot(clock()).date, context, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            callbackFlow<Unit> {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) { trySend(Unit) }
                }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_DATE_CHANGED)
                    addAction(Intent.ACTION_TIME_CHANGED)
                    addAction(Intent.ACTION_TIMEZONE_CHANGED)
                }
                ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
                trySend(Unit) // Includes every resume, even after days in the background.
                awaitClose { context.unregisterReceiver(receiver) }
            }.collectLatest {
                while (true) {
                    val snapshot = calendarDateSnapshot(currentClock.value())
                    value = snapshot.date
                    delay(snapshot.nextRefreshMillis)
                }
            }
        }
    }
}
