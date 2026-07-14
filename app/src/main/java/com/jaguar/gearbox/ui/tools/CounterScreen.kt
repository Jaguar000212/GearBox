package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.theme.LocalHapticsEnabled
import androidx.glance.appwidget.updateAll
import com.jaguar.gearbox.widget.CounterWidget
import kotlinx.coroutines.launch

private const val KEY_COUNT = "counter.count"
private val STEP_OPTIONS = listOf(1, 5, 10)

@Composable
fun CounterScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()
    val store = remember { SimplePrefsStore(context) }
    var count by rememberSaveable { mutableIntStateOf(store.getInt(KEY_COUNT, 0)) }
    var step by rememberSaveable { mutableIntStateOf(1) }

    // The Counter home-screen widget writes to the same key while this screen is backgrounded;
    // pick that up as soon as the user returns instead of showing a stale in-memory value.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                count = store.getInt(KEY_COUNT, count)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun update(newCount: Int) {
        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        count = newCount
        store.putInt(KEY_COUNT, newCount)
        scope.launch { CounterWidget().updateAll(context) }
    }

    ToolScaffold(
        title = "Counter",
        icon = Tools.byRoute(Tools.ROUTE_COUNTER)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            STEP_OPTIONS.forEach { option ->
                FilterChip(
                    selected = step == option,
                    onClick = { step = option },
                    label = { Text("+$option") },
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = { update(count - step) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrement")
            }
            FilledTonalButton(onClick = { update(count + step) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, contentDescription = "Increment")
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { update(0) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text("  Reset")
        }
    }
}
