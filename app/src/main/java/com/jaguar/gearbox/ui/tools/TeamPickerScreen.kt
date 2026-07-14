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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.SimplePrefsStore
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.StringListSaver
import com.jaguar.gearbox.ui.components.ToolScaffold

private const val KEY_NAMES = "team_picker.names"
private const val NAME_DELIMITER = "\n"

@Composable
fun TeamPickerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
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
        store.putString(KEY_NAMES, updated.joinToString(NAME_DELIMITER))
    }

    fun addName() {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            persistNames(names + trimmed)
            newName = ""
        }
    }

    ToolScaffold(
        title = "Team / Name Picker",
        icon = Tools.byRoute(Tools.ROUTE_TEAM_PICKER)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Add a name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { addName() }) {
                Icon(Icons.Filled.Add, contentDescription = "Add name")
            }
        }

        if (names.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text("  $name", style = MaterialTheme.typography.bodyLarge)
                            }
                            IconButton(onClick = {
                                persistNames(names.toMutableList().also { it.removeAt(index) })
                                winner = null
                                teams = emptyList()
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove $name")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { winner = names.random() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null)
                Text("  Pick one")
            }

            if (winner != null) {
                Spacer(Modifier.height(12.dp))
                ResultCard(text = "Picked: $winner")
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Number of teams", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { if (teamCount > 2) teamCount-- }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Fewer teams")
                    }
                    Text(
                        text = teamCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    FilledTonalIconButton(onClick = { if (teamCount < names.size) teamCount++ }) {
                        Icon(Icons.Filled.Add, contentDescription = "More teams")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
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
                    onCopy = { context.copyToClipboard("Teams", summary) },
                    onShare = { context.shareText(summary) },
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Text(
                "Add at least two names to pick or split into teams.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
