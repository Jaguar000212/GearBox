package com.jaguar.gearbox.widget

import android.content.Context
import androidx.compose.runtime.Composable
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

private const val KEY_COUNT = "counter.count"
private val KEY_OP = ActionParameters.Key<String>("op")

/**
 * Home-screen widget mirroring the Counter tool's tally. Reads/writes the same
 * [SimplePrefsStore] key as `CounterScreen`, so the widget and the open app never disagree about
 * the count - whichever one changes it last wins, and the other picks it up next time it updates.
 */
class CounterWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = SimplePrefsStore(context)
        provideContent {
            val count = store.getInt(KEY_COUNT, 0)
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFFFFF9EB)))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Counter",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        color = ColorProvider(Color(0xFF6B5F10)),
                    ),
                )
                Text(
                    text = count.toString(),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = ColorProvider(Color(0xFF1D1C13)),
                    ),
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Row(horizontalAlignment = Alignment.CenterHorizontally) {
                    WidgetActionText(label = "−", op = "dec")
                    Spacer(modifier = GlanceModifier.width(16.dp))
                    WidgetActionText(label = "Reset", op = "reset")
                    Spacer(modifier = GlanceModifier.width(16.dp))
                    WidgetActionText(label = "+", op = "inc")
                }
            }
        }
    }
}

@Composable
private fun WidgetActionText(label: String, op: String) {
    Text(
        text = label,
        style = TextStyle(
            fontWeight = FontWeight.Bold,
            color = ColorProvider(Color(0xFF6B5F10)),
        ),
        modifier = GlanceModifier.clickable(
            actionRunCallback<CounterWidgetAction>(actionParametersOf(KEY_OP to op)),
        ),
    )
}

class CounterWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val store = SimplePrefsStore(context)
        val newCount = when (parameters[KEY_OP]) {
            "inc" -> store.getInt(KEY_COUNT, 0) + 1
            "dec" -> store.getInt(KEY_COUNT, 0) - 1
            else -> 0
        }
        store.putInt(KEY_COUNT, newCount)
        CounterWidget().update(context, glanceId)
    }
}

class CounterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CounterWidget()
}
