package com.jaguar.gearbox.ui.tools

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
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.IntListSaver
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiceRollScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var diceCount by rememberSaveable { mutableIntStateOf(1) }
    var values by rememberSaveable(stateSaver = IntListSaver) { mutableStateOf(emptyList<Int>()) }
    val result = values.takeIf { it.isNotEmpty() }?.let {
        "Dice Roll ($diceCount six-sided dice): ${it.joinToString(", ")}\nTotal: ${it.sum()}"
    }

    fun changeCount(count: Int) {
        diceCount = count.coerceIn(1, 6)
        values = emptyList()
    }

    ToolScaffold(
        title = "Dice Roll",
        icon = Tools.byRoute(Tools.ROUTE_DICE)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Number of dice", style = MaterialTheme.typography.titleMedium)
                Text("Choose 1–6 six-sided dice.", style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center) {
                    FilledTonalIconButton(onClick = { changeCount(diceCount - 1) }, enabled = diceCount > 1) {
                        Icon(Icons.Filled.Remove, contentDescription = "Fewer dice")
                    }
                    Text(diceCount.toString(), style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 24.dp).semantics {
                            contentDescription = "$diceCount dice selected"
                        })
                    FilledTonalIconButton(onClick = { changeCount(diceCount + 1) }, enabled = diceCount < 6) {
                        Icon(Icons.Filled.Add, contentDescription = "More dice")
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (values.isEmpty()) "Ready to roll" else "Your roll", style = MaterialTheme.typography.titleMedium)
                if (values.isEmpty()) {
                    Text("Roll to see each die and the total. Changing the count clears the previous roll.",
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        values.forEachIndexed { index, value ->
                            Surface(shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 64.dp)
                                    .clearAndSetSemantics { contentDescription = "Die ${index + 1}: $value" }) {
                                Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                        }
                    }
                    Text("Total: ${values.sum()}", style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { values = List(diceCount) { Random.nextInt(1, 7) } }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Casino, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (values.isEmpty()) "Roll" else "Roll again")
        }
        ToolActions(
            onCopy = { result?.let { context.copyToClipboard("Dice roll", it) } },
            onShare = { result?.let { context.shareText(it) } },
            onClear = { values = emptyList() },
            resultEnabled = result != null,
            clearEnabled = result != null,
        )
    }
}
