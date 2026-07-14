package com.jaguar.gearbox.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.logic.catchUpTimerState
import com.jaguar.gearbox.logic.formatDurationCompact

private const val KEY_MODE = "timer_stopwatch.mode"
private const val KEY_SW_ELAPSED = "timer_stopwatch.sw_elapsed"
private const val KEY_SW_RUNNING = "timer_stopwatch.sw_running"
private const val KEY_LAPS = "timer_stopwatch.laps"
private const val KEY_TIMER_REMAINING = "timer_stopwatch.timer_remaining"
private const val KEY_TIMER_RUNNING = "timer_stopwatch.timer_running"
private const val KEY_TIMER_FINISHED = "timer_stopwatch.timer_finished"
private const val KEY_SAVED_AT = "timer_stopwatch.saved_at"
private const val MODE_STOPWATCH = "STOPWATCH"

private val KEY_OP = ActionParameters.Key<String>("op")

/**
 * Home-screen snapshot of the Timer/Stopwatch tool. Unlike the in-app screen this can't tick
 * every second - Glance widgets only redraw when explicitly updated - so this shows the time as
 * of the last save (an interaction here, in the app, or the catch-up math applied at render time)
 * rather than a live-animating clock.
 */
class TimerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = SimplePrefsStore(context)
        provideContent {
            val isStopwatch = store.getString(KEY_MODE, MODE_STOPWATCH) == MODE_STOPWATCH
            val snapshot = catchUpTimerState(
                stopwatchElapsedMillis = store.getLong(KEY_SW_ELAPSED, 0L),
                stopwatchRunning = store.getBoolean(KEY_SW_RUNNING, false),
                timerRemainingMillis = store.getLong(KEY_TIMER_REMAINING, 0L),
                timerRunning = store.getBoolean(KEY_TIMER_RUNNING, false),
                timerFinished = store.getBoolean(KEY_TIMER_FINISHED, false),
                savedAt = store.getLong(KEY_SAVED_AT, 0L),
                now = System.currentTimeMillis(),
            )
            val running =
                if (isStopwatch) store.getBoolean(KEY_SW_RUNNING, false) else snapshot.timerRunning
            val displayMillis =
                if (isStopwatch) snapshot.stopwatchElapsedMillis else snapshot.timerRemainingMillis
            val canStart = isStopwatch || running || snapshot.timerRemainingMillis > 0

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFFFFF9EB)))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isStopwatch) "Stopwatch" else "Timer",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(Color(0xFF6B5F10)),
                    ),
                )
                Text(
                    text = formatDurationCompact(displayMillis),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = ColorProvider(Color(0xFF1D1C13)),
                    ),
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Row(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (running) "Pause" else if (canStart) "Start" else "Open app",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFF6B5F10)),
                        ),
                        modifier = GlanceModifier.clickable(
                            actionRunCallback<TimerWidgetAction>(actionParametersOf(KEY_OP to "toggle")),
                        ),
                    )
                    Spacer(modifier = GlanceModifier.width(16.dp))
                    Text(
                        text = "Reset",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFF6B5F10)),
                        ),
                        modifier = GlanceModifier.clickable(
                            actionRunCallback<TimerWidgetAction>(actionParametersOf(KEY_OP to "reset")),
                        ),
                    )
                }
            }
        }
    }
}

class TimerWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val store = SimplePrefsStore(context)
        val isStopwatch = store.getString(KEY_MODE, MODE_STOPWATCH) == MODE_STOPWATCH
        val now = System.currentTimeMillis()

        // Catch up first so pausing a running clock freezes it at the correct point, not wherever
        // it was left at the last periodic in-app save.
        val snapshot = catchUpTimerState(
            stopwatchElapsedMillis = store.getLong(KEY_SW_ELAPSED, 0L),
            stopwatchRunning = store.getBoolean(KEY_SW_RUNNING, false),
            timerRemainingMillis = store.getLong(KEY_TIMER_REMAINING, 0L),
            timerRunning = store.getBoolean(KEY_TIMER_RUNNING, false),
            timerFinished = store.getBoolean(KEY_TIMER_FINISHED, false),
            savedAt = store.getLong(KEY_SAVED_AT, 0L),
            now = now,
        )

        when (parameters[KEY_OP]) {
            "toggle" -> if (isStopwatch) {
                val running = store.getBoolean(KEY_SW_RUNNING, false)
                store.edit {
                    putLong(KEY_SW_ELAPSED, snapshot.stopwatchElapsedMillis)
                    putBoolean(KEY_SW_RUNNING, !running)
                    putLong(KEY_SAVED_AT, now)
                }
            } else {
                // No text input on a widget: this can only pause/resume a timer already
                // configured from inside the app, not start one from a duration of zero.
                store.edit {
                    putLong(KEY_TIMER_REMAINING, snapshot.timerRemainingMillis)
                    putBoolean(
                        KEY_TIMER_RUNNING,
                        !snapshot.timerRunning && snapshot.timerRemainingMillis > 0,
                    )
                    putBoolean(KEY_TIMER_FINISHED, snapshot.timerFinished)
                    putLong(KEY_SAVED_AT, now)
                }
            }

            "reset" -> if (isStopwatch) {
                store.edit {
                    putLong(KEY_SW_ELAPSED, 0L)
                    putBoolean(KEY_SW_RUNNING, false)
                    putString(KEY_LAPS, "")
                    putLong(KEY_SAVED_AT, now)
                }
            } else {
                store.edit {
                    putLong(KEY_TIMER_REMAINING, 0L)
                    putBoolean(KEY_TIMER_RUNNING, false)
                    putBoolean(KEY_TIMER_FINISHED, false)
                    putLong(KEY_SAVED_AT, now)
                }
            }
        }

        TimerWidget().update(context, glanceId)
    }
}

class TimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimerWidget()
}
