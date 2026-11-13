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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.jaguar.gearbox.data.CounterStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.theme.LocalHapticsEnabled
import com.jaguar.gearbox.widget.CounterWidget
import kotlinx.coroutines.launch

private val STEP_OPTIONS = listOf(1, 5, 10)

@Composable
fun CounterScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val scope = rememberCoroutineScope()
    val store = remember { CounterStore(context) }
    var count by remember { mutableIntStateOf(store.count) }
    var limitReached by remember { mutableStateOf(store.limitReached) }
    var step by rememberSaveable { mutableIntStateOf(1) }

    DisposableEffect(store) {
        val stopObserving = store.observe {
            count = store.count
            limitReached = store.limitReached
        }
        // Register before reading so a concurrent widget update cannot be missed.
        count = store.count
        limitReached = store.limitReached
        onDispose { stopObserving() }
    }

    fun update(delta: Int? = null) {
        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (delta == null) store.reset() else store.adjust(delta)
        count = store.count
        limitReached = store.limitReached
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
        if (limitReached) {
            Text("Counter limit reached. The count has not changed.", color = MaterialTheme.colorScheme.error)
        }
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
            FilledTonalButton(onClick = { update(-step) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrement")
            }
            FilledTonalButton(onClick = { update(step) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, contentDescription = "Increment")
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { update() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text("  Reset")
        }
    }
}
