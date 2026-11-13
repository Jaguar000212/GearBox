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
import android.content.Intent
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.action.actionStartActivity
import com.jaguar.gearbox.MainActivity
import com.jaguar.gearbox.data.EXTRA_SHORTCUT_ROUTE
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.timer.ClockRepository
import com.jaguar.gearbox.logic.formatDurationCompact
import com.jaguar.gearbox.logic.toggleCountdown
import com.jaguar.gearbox.logic.resetCountdown

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
        provideContent {
            val snapshot = ClockRepository.snapshot(context)
            val isStopwatch = snapshot.mode == MODE_STOPWATCH
            val running = if (isStopwatch) snapshot.stopwatchRunning else snapshot.timerRunning
            val displayMillis = if (isStopwatch) snapshot.stopwatchElapsedMillis else snapshot.timerRemainingMillis
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
                            if (canStart) actionRunCallback<TimerWidgetAction>(actionParametersOf(KEY_OP to "toggle"))
                            else actionStartActivity(Intent(context, MainActivity::class.java)
                                .putExtra(EXTRA_SHORTCUT_ROUTE, Tools.ROUTE_TIMER_STOPWATCH)),
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
        ClockRepository.update(context) { state ->
            val isStopwatch = state.mode == MODE_STOPWATCH
            when (parameters[KEY_OP]) {
                "toggle" -> if (isStopwatch) state.copy(stopwatchRunning = !state.stopwatchRunning)
                else toggleCountdown(state, allowNewDuration = false)
                "reset" -> if (isStopwatch) state.copy(
                    stopwatchRunning = false, stopwatchElapsedMillis = 0, laps = emptyList(),
                ) else resetCountdown(state)
                else -> state
            }
        }
        TimerWidget().updateAll(context)
    }
}

class TimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimerWidget()
}
