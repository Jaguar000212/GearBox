package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.IntListSaver
import com.jaguar.gearbox.ui.components.ToolScaffold

private const val KEY_CALLED = "tambola.called"
private const val KEY_LAST_CALLED = "tambola.last_called"

@Composable
fun TambolaScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SimplePrefsStore(context) }
    var calledNumbers by rememberSaveable(stateSaver = IntListSaver) {
        mutableStateOf(store.getIntList(KEY_CALLED))
    }
    var lastCalled by rememberSaveable { mutableStateOf(store.getInt(KEY_LAST_CALLED, 0)) }
    var showResetConfirm by rememberSaveable { mutableStateOf(false) }

    fun persistGame(numbers: List<Int>, last: Int) {
        store.edit {
            putString(KEY_CALLED, numbers.joinToString(","))
            putInt(KEY_LAST_CALLED, last)
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset game?") },
            text = { Text("This clears all ${calledNumbers.size} called numbers. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    calledNumbers = emptyList()
                    lastCalled = 0
                    persistGame(calledNumbers, lastCalled)
                    showResetConfirm = false
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
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
                        persistGame(calledNumbers, lastCalled)
                    }
                },
                enabled = calledNumbers.size < 90,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Casino, contentDescription = null)
                Text("  Call number")
            }
            OutlinedButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Text(" Reset")
            }
        }

        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(10),
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
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
                    // Fills whatever width the grid computes for 10 columns instead of a fixed
                    // 32dp, which clipped past the edge of the screen on narrow (360dp) devices.
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                ) {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (called) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
