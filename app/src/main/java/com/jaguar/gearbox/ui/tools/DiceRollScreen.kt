package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.IntListSaver
import com.jaguar.gearbox.ui.components.ToolScaffold
import kotlin.random.Random

@Composable
fun DiceRollScreen(onNavigateBack: () -> Unit) {
    var diceCount by rememberSaveable { mutableIntStateOf(1) }
    var values by rememberSaveable(stateSaver = IntListSaver) { mutableStateOf(emptyList()) }

    ToolScaffold(
        title = "Dice Roll",
        icon = Tools.byRoute(Tools.ROUTE_DICE)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Number of dice", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { if (diceCount > 1) diceCount-- }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Fewer dice")
                }
                Text(
                    text = diceCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                FilledTonalIconButton(onClick = { if (diceCount < 6) diceCount++ }) {
                    Icon(Icons.Filled.Add, contentDescription = "More dice")
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        if (values.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(values) { value ->
                    Card(modifier = Modifier.size(64.dp)) {
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Total: ${values.sum()}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { values = List(diceCount) { Random.nextInt(1, 7) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Casino, contentDescription = null)
            Text("  Roll")
        }
    }
}
