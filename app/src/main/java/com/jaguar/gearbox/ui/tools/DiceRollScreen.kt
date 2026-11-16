package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.R

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.RandomResultSnapshots
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.IntListSaver
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiceRollScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var runId by rememberSaveable { mutableStateOf("") }
    var diceCount by rememberSaveable { mutableIntStateOf(1) }
    var values by rememberSaveable(stateSaver = IntListSaver) { mutableStateOf(emptyList<Int>()) }
    val diceLabel = context.resources.getQuantityString(R.plurals.game_dice_noun, diceCount)
    val result = values.takeIf { it.isNotEmpty() }?.let {
        context.getString(R.string.game_dice_roll_1_s_six_sided_2_s_3_s_ntotal_4_s, diceCount, diceLabel, it.joinToString(", "), it.sum())
    }

    fun changeCount(count: Int) {
        diceCount = count.coerceIn(1, 6)
        values = emptyList()
    }

    val history = rememberToolHistory(Tools.ROUTE_DICE, mapOf("dice" to diceCount.toString(), "_dice" to values.joinToString(","), "_run" to runId), result,
        onRestore = { saved ->
            changeCount(saved["dice"]?.toIntOrNull() ?: 1)
            values = RandomResultSnapshots.dice(saved).orEmpty()
            runId = saved["_run"].orEmpty()
        },
        recordingDelayMillis = 0L,
        modeInputs = mapOf("dice" to diceCount.toString()),
        onRestoreMode = { saved -> changeCount(saved["dice"]?.toIntOrNull() ?: 1) })
    ToolScaffold(
        title = context.getString(R.string.game_dice_roll),
        icon = Tools.byRoute(Tools.ROUTE_DICE)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center) {
                    FilledTonalIconButton(onClick = { changeCount(diceCount - 1) }, enabled = diceCount > 1) {
                        Icon(Icons.Filled.Remove, contentDescription = context.getString(R.string.game_fewer_dice))
                    }
                    Text(context.getString(R.string.game_dice_count, diceCount, diceLabel), style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 24.dp).semantics {
                            contentDescription = context.getString(R.string.game_1_s_2_s_selected, diceCount, diceLabel)
                        })
                    FilledTonalIconButton(onClick = { changeCount(diceCount + 1) }, enabled = diceCount < 6) {
                        Icon(Icons.Filled.Add, contentDescription = context.getString(R.string.game_more_dice))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (values.isEmpty()) context.getString(R.string.game_ready_to_roll) else context.getString(R.string.game_your_roll), style = MaterialTheme.typography.titleMedium)
                if (values.isEmpty()) {
                    Text(context.getString(R.string.game_roll_to_see_each_die_and_the_total_changing_the_count_clea),
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        values.forEachIndexed { index, value ->
                            Surface(shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 64.dp)
                                    .clearAndSetSemantics { contentDescription = context.getString(R.string.game_die_1_s_2_s, index + 1, value) }) {
                                Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }
                    }
                    Text(context.getString(R.string.game_total_1_s_122, values.sum()), style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { runId = java.util.UUID.randomUUID().toString(); values = List(diceCount) { Random.nextInt(1, 7) } }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Casino, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (values.isEmpty()) context.getString(R.string.game_roll) else context.getString(R.string.game_roll_again))
        }
        ToolActions(
            onCopy = { result?.let { context.copyToClipboard(context.getString(R.string.game_dice_roll_125), it) } },
            onShare = { result?.let { context.shareText(it) } },
            onClear = { values = emptyList() },
            resultEnabled = result != null,
            clearEnabled = result != null,
        )
    }
}
