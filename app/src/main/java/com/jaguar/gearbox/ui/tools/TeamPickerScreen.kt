package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.StringListSaver
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold

private const val KEY_NAMES = "team_picker.names"
private const val NAME_DELIMITER = "\n"

@Composable
fun TeamPickerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val store = remember { SimplePrefsStore(context) }
    var names by rememberSaveable(stateSaver = StringListSaver) {
        val saved = store.getString(KEY_NAMES, "")
        mutableStateOf(if (saved.isEmpty()) emptyList() else saved.split(NAME_DELIMITER))
    }
    var newName by rememberSaveable { mutableStateOf("") }
    var teamCount by rememberSaveable { mutableStateOf(2) }
    var winner by rememberSaveable { mutableStateOf<String?>(null) }
    var teams by rememberSaveable(stateSaver = StringListSaver) { mutableStateOf(emptyList()) }

    fun persistNames(updated: List<String>) {
        names = updated
        winner = null
        teams = emptyList()
        teamCount = teamCount.coerceIn(2, updated.size.coerceAtLeast(2))
        store.putString(KEY_NAMES, updated.joinToString(NAME_DELIMITER))
    }

    fun addName() {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            persistNames(names + trimmed)
            newName = ""
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear saved names?") },
            text = { Text("This removes every participant and the current results. You cannot undo it.") },
            confirmButton = { TextButton(onClick = {
                persistNames(emptyList()); newName = ""; confirmClear = false
            }) { Text("Clear names") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }

    ToolScaffold(
        title = "Team / Name Picker",
        icon = Tools.byRoute(Tools.ROUTE_TEAM_PICKER)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Text("Let chance choose", style = MaterialTheme.typography.titleLarge)
        Text("Pick one name or make balanced teams. Repeated names count as separate entries.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Add a name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { addName() }, enabled = newName.isNotBlank()) {
                Icon(Icons.Filled.Add, contentDescription = "Add name")
            }
        }

        if (names.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("Participants (${names.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    names.forEachIndexed { index, name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(start = 8.dp))
                            }
                            IconButton(onClick = {
                                persistNames(names.toMutableList().also { it.removeAt(index) })
                                winner = null
                                teams = emptyList()
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove participant ${index + 1}: $name")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { focusManager.clearFocus(); winner = names.random() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null)
                Text("  Pick one")
            }

            if (winner != null) {
                Spacer(Modifier.height(12.dp))
                val picked = "Picked: $winner"
                ResultCard(text = picked,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    onCopy = { context.copyToClipboard("Picked name", picked) },
                    onShare = { context.shareText(picked) })
            }

            Spacer(Modifier.height(20.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Number of teams", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { if (teamCount > 2) { teamCount--; teams = emptyList() } }, enabled = teamCount > 2) {
                        Icon(Icons.Filled.Remove, contentDescription = "Fewer teams")
                    }
                    Text(
                        text = teamCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    FilledTonalIconButton(onClick = { if (teamCount < names.size) { teamCount++; teams = emptyList() } }, enabled = teamCount < names.size) {
                        Icon(Icons.Filled.Add, contentDescription = "More teams")
                    }
                }
            }
            Text(if (names.size < 2) "Add one more name to make teams. You can already pick one."
                else "Team sizes differ by at most one participant.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    val shuffled = names.shuffled()
                    val groups = List(teamCount) { mutableListOf<String>() }
                    shuffled.forEachIndexed { index, name -> groups[index % teamCount].add(name) }
                    teams = groups.mapIndexed { index, group ->
                        "Team ${index + 1}: ${
                            group.joinToString(", ")
                        }"
                    }
                },
                enabled = teamCount in 2..names.size,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Groups, contentDescription = null)
                Text("  Split into teams")
            }

            if (teams.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val summary = teams.joinToString("\n")
                ResultCard(
                    text = summary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    onCopy = { context.copyToClipboard("Teams", summary) },
                    onShare = { context.shareText(summary) },
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                "Add one name to pick, or at least two to split into teams. Names are saved on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        ToolActions(onClear = { confirmClear = true }, clearEnabled = names.isNotEmpty() || newName.isNotEmpty())
    }
}
