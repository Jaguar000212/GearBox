package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold

private val calledNumbersSaver: Saver<MutableState<List<Int>>, IntArray> = Saver(
    save = { it.value.toIntArray() },
    restore = { mutableStateOf(it.toList()) },
)

@Composable
fun TambolaScreen(onNavigateBack: () -> Unit) {
    var calledNumbers by rememberSaveable(saver = calledNumbersSaver) { mutableStateOf(emptyList()) }
    var lastCalled by rememberSaveable { mutableStateOf(0) }

    ToolScaffold(
        title = "Tambola Numbers",
        icon = Tools.byRoute(Tools.ROUTE_TAMBOLA)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (lastCalled == 0) "-" else lastCalled.toString(),
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
        }

        if (calledNumbers.size > 1) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Skip the most recent number (already shown large above); show the few before it.
                calledNumbers.reversed().drop(1).take(5).forEach { number ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Text(
                            text = number.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "${calledNumbers.size} of 90 numbers called",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    val remaining = (1..90).filter { it !in calledNumbers }
                    if (remaining.isNotEmpty()) {
                        val next = remaining.random()
                        calledNumbers = calledNumbers + next
                        lastCalled = next
                    }
                },
                enabled = calledNumbers.size < 90,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Casino, contentDescription = null)
                Text("  Call number")
            }
            OutlinedButton(
                onClick = {
                    calledNumbers = emptyList()
                    lastCalled = 0
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text(" Reset")
            }
        }

        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(10),
            modifier = Modifier.fillMaxWidth().height(360.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(90) { index ->
                val number = index + 1
                val called = number in calledNumbers
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (called) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    modifier = Modifier.size(32.dp),
                ) {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (called) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
