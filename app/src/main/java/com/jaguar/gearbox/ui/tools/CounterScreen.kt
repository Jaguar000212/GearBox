package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.R

import com.jaguar.gearbox.ui.components.ToolFocusView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.jaguar.gearbox.ui.components.rememberToolHistory
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

    var stepMenu by remember { mutableStateOf(false) }
    var focusView by rememberSaveable { mutableStateOf(false) }
    val history = rememberToolHistory(Tools.ROUTE_COUNTER,
        mapOf("count" to count.toString(), "step" to step.toString()), context.getString(R.string.game_count_1_s, count), onRestore = { saved ->
        saved["count"]?.toIntOrNull()?.let { restored ->
            store.restore(restored)
            count = store.count; limitReached = store.limitReached
            scope.launch { CounterWidget().updateAll(context) }
        }
        step = saved["step"]?.toIntOrNull()?.takeIf { it in STEP_OPTIONS } ?: 1
    }, restoreLastResult = false, recordingDelayMillis = 0L, modeInputs = mapOf("step" to step.toString()), onRestoreMode = { saved ->
        step = saved["step"]?.toIntOrNull()?.takeIf { it in STEP_OPTIONS } ?: 1
    })
    if (focusView) ToolFocusView(context.getString(R.string.game_counter), onExit = { focusView = false }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(count.toString(), style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { update(-step) }) { Text(context.getString(R.string.game_1_s, step)) }
                    Button(onClick = { update(step) }) { Text(context.getString(R.string.game_1_s_3, step)) }
                }
                if (limitReached) Text(context.getString(R.string.game_counter_limit_reached), color = MaterialTheme.colorScheme.error)
            }
        }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(context.getString(R.string.game_reset_count)) },
            text = { Text(context.getString(R.string.game_the_current_count_1_s_will_be_set_to_zero, count)) },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; update() }) { Text(context.getString(R.string.game_reset_count_7)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(context.getString(R.string.game_keep_counting)) }
            },
        )
    }

    ToolScaffold(
        title = context.getString(R.string.game_counter),
        icon = Tools.byRoute(Tools.ROUTE_COUNTER)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
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
                Text(context.getString(R.string.game_count), style = MaterialTheme.typography.labelLarge,
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
            Text(context.getString(R.string.game_counter_limit_reached_the_count_has_not_changed), color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Spacer(Modifier.height(12.dp))
        ToolActions(
            onCopy = { context.copyToClipboard(context.getString(R.string.game_count), store.count.toString()) },
            onShare = { context.shareText(context.getString(R.string.game_count_1_s, store.count)) },
        )
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box {
                TextButton(onClick = { stepMenu = true }) { Text(context.getString(R.string.game_step_1_s, step)) }
                DropdownMenu(expanded = stepMenu, onDismissRequest = { stepMenu = false }) {
                    STEP_OPTIONS.forEach { option ->
                        DropdownMenuItem(text = { Text(option.toString()) }, onClick = { step = option; stepMenu = false })
                    }
                }
            }
            TextButton(onClick = { focusView = true }) { Text(context.getString(R.string.game_focus_view)) }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = { update(-step) },
                modifier = Modifier.weight(1f).heightIn(min = 64.dp).semantics { contentDescription = context.getString(R.string.game_decrease_by_1_s, step) }) {
                Icon(Icons.Filled.Remove, contentDescription = null)
                Text(context.getString(R.string.game_1_s_14, step), style = MaterialTheme.typography.titleLarge)
            }
            Button(onClick = { update(step) },
                modifier = Modifier.weight(1f).heightIn(min = 64.dp).semantics { contentDescription = context.getString(R.string.game_increase_by_1_s, step) }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(context.getString(R.string.game_1_s_14, step), style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = { if (count != 0) confirmReset = true else update() },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text(context.getString(R.string.game_reset))
        }
    }
}
