package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.jaguar.gearbox.data.CounterStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolActions
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

    var confirmReset by rememberSaveable { mutableStateOf(false) }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset count?") },
            text = { Text("The current count ($count) will be set to zero.") },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; update() }) { Text("Reset count") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Keep counting") }
            },
        )
    }

    ToolScaffold(
        title = "Counter",
        icon = Tools.byRoute(Tools.ROUTE_COUNTER)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Spacer(Modifier.height(16.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Count", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = count.toString(),
                    style = (if (count.toString().length > 7) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayLarge).copy(fontFamily = FontFamily.Monospace),
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                )
            }
        }
        if (limitReached) {
            Text("Counter limit reached. The count has not changed.", color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Spacer(Modifier.height(12.dp))
        ToolActions(
            onCopy = { context.copyToClipboard("Count", store.count.toString()) },
            onShare = { context.shareText("Count: ${store.count}") },
        )
        Spacer(Modifier.height(24.dp))
        Text("Step", style = MaterialTheme.typography.titleSmall)
        Text("Amount added or removed with each tap", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            STEP_OPTIONS.forEach { option ->
                FilterChip(
                    selected = step == option,
                    onClick = { step = option },
                    label = { Text(option.toString()) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = { update(-step) },
                modifier = Modifier.weight(1f).heightIn(min = 64.dp).semantics { contentDescription = "Decrease by $step" }) {
                Icon(Icons.Filled.Remove, contentDescription = null)
                Text(" $step", style = MaterialTheme.typography.titleLarge)
            }
            Button(onClick = { update(step) },
                modifier = Modifier.weight(1f).heightIn(min = 64.dp).semantics { contentDescription = "Increase by $step" }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(" $step", style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = { if (count != 0) confirmReset = true else update() },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text("  Reset")
        }
    }
}
