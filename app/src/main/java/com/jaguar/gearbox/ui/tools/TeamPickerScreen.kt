package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.R
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.data.parseWeightedPickerSnapshot
import com.jaguar.gearbox.data.validTeamPickerMode

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
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
import com.jaguar.gearbox.ui.components.GroupPresentation
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory

private const val KEY_NAMES = "team_picker.names"
private const val NAME_DELIMITER = "\n"

@Composable
fun TeamPickerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var presenting by rememberSaveable { mutableStateOf(false) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val store = remember { SimplePrefsStore(context, gameSession = true) }
    var names by rememberSaveable(stateSaver = StringListSaver) {
        val saved = store.getString(KEY_NAMES, "")
        mutableStateOf(if (saved.isEmpty()) emptyList() else saved.split(NAME_DELIMITER))
    }
    var weights by rememberSaveable(stateSaver = StringListSaver) {
        val saved = store.getString("team_picker.weights", "").split("\n")
        val owner = store.getString("team_picker.weight_names", "")
        mutableStateOf(if(owner==names.joinToString("\n") && saved.size==names.size && saved.all {parsePickerWeight(it)!=null}) saved else List(names.size){"1"})
    }
    var weightedWinner by rememberSaveable { mutableStateOf<String?>(null) }
    var includeWeights by rememberSaveable { mutableStateOf(true) }
    var newName by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf("pick") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var teamCount by rememberSaveable { mutableStateOf(2) }
    var winner by rememberSaveable { mutableStateOf<String?>(null) }
    var teams by rememberSaveable(stateSaver = StringListSaver) { mutableStateOf(emptyList()) }

    fun persistNames(updated: List<String>, updatedWeights: List<String> = List(updated.size) { weights.getOrNull(it) ?: "1" }) {
        weights = updatedWeights
        includeWeights = true
        weightedWinner = null
        names = updated
        winner = null
        teams = emptyList()
        teamCount = teamCount.coerceIn(2, updated.size.coerceAtLeast(2))
        store.edit {
            putString(KEY_NAMES, updated.joinToString(NAME_DELIMITER))
            putString("team_picker.weights", weights.joinToString("\n"))
            putString("team_picker.weight_names", updated.joinToString(NAME_DELIMITER))
        }
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
            title = { Text(context.getString(R.string.game_clear_names_and_results)) },
            text = { Text(context.getString(R.string.game_this_clears_saved_participants_any_name_you_are_typing_and)) },
            confirmButton = { TextButton(onClick = {
                persistNames(emptyList()); newName = ""; confirmClear = false
            }) { Text(context.getString(R.string.game_clear_names)) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(context.getString(R.string.game_cancel)) } },
        )
    }

    val weightedInputs = buildMap {
        put("participants",names.joinToString(NAME_DELIMITER));put("mode",mode);put("teams",teamCount.toString())
        if(mode=="weighted" && includeWeights) put("weights",weights.joinToString("\n"))
    }
    val weightedValid = names.size in 1..200 && names.all(::validWeightedName) && weights.size==names.size && weights.all {parsePickerWeight(it)!=null}
    val history = rememberToolHistory(Tools.ROUTE_TEAM_PICKER,
        weightedInputs,
        if(mode=="weighted") weightedWinner else if (mode == "pick") winner?.let { context.getString(R.string.game_picked_1_s, it) } else teams.takeIf { it.isNotEmpty() }?.joinToString("\n"),
        restoreLastResult = false,
        onRestore = { saved ->
            if(saved["mode"]=="weighted") {
                parseWeightedPickerSnapshot(saved)?.let { value ->
                    persistNames(value.participants,value.weights.map(Int::toString))
                    includeWeights=value.includesWeights
                    mode="weighted";teamCount=value.teams
                }
            } else {
            val restoredNames=saved["participants"].orEmpty().split(NAME_DELIMITER).filter { it.isNotBlank() }
            persistNames(restoredNames,List(restoredNames.size){"1"})
            mode = if (saved["mode"] == "teams") "teams" else "pick"
            teamCount = (saved["teams"]?.toIntOrNull() ?: 2).coerceIn(2, names.size.coerceAtLeast(2))
            }
        }, modeInputs = mapOf("mode" to mode, "teams" to teamCount.toString()),
        onRestoreMode = { saved ->
            if(validTeamPickerMode(saved)) {
                mode=saved.getValue("mode")
                val restoredCount=saved.getValue("teams").toInt().coerceIn(2,names.size.coerceAtLeast(2))
                if(restoredCount!=teamCount) teams=emptyList()
                teamCount=restoredCount
            }
        })
    val presentationPages = if(mode=="weighted") weightedWinner?.let {listOf(it)}.orEmpty() else if (mode == "pick") winner?.let { listOf(context.getString(R.string.game_picked_1_s, it)) }.orEmpty() else teams
    if (presenting && presentationPages.isNotEmpty()) {
        GroupPresentation(presentationPages, onClose = { presenting = false })
        return // Do not leave participant/result semantics beneath the hidden presentation.
    }
    ToolScaffold(
        title = context.getString(R.string.game_team_name_picker),
        icon = Tools.byRoute(Tools.ROUTE_TEAM_PICKER)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        Text(context.getString(R.string.game_let_chance_choose), style = MaterialTheme.typography.titleLarge)
        Text(context.getString(R.string.game_pick_one_name_or_make_balanced_teams_repeated_names_count),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(mode == "pick", { mode = "pick" }, label = { Text(context.getString(R.string.game_pick_a_name)) })
            FilterChip(mode == "teams", { mode = "teams" }, label = { Text(context.getString(R.string.game_make_teams)) })
        }
        FilterChip(mode=="weighted", {mode="weighted"}, label={Text(context.getString(R.string.weighted_mode))})
        if(mode=="weighted") {
            Text(context.getString(R.string.weighted_help))
            if(!weightedValid) Text(context.getString(R.string.weighted_invalid))
            Button(onClick={
                val parsed=weights.map {parsePickerWeight(it)!!}
                val index=pickWeightedIndex(parsed)
                weightedWinner=context.getString(R.string.weighted_result,index+1,names[index],parsed[index],weightedTotal(parsed)!!)
            },enabled=weightedValid) {Text(context.getString(R.string.weighted_pick))}
            weightedWinner?.let {result -> ResultCard(text=result,onCopy={context.copyToClipboard(context.getString(R.string.weighted_mode),result)},onShare={context.shareText(result)})}
        }
        OutlinedButton(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
            Text(context.getString(R.string.game_participants_1_s_edit, names.size))
        }

        if (names.isNotEmpty() && mode!="weighted") {
            if (mode == "pick") {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { focusManager.clearFocus(); winner = names.random() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null)
                Text(context.getString(R.string.game_pick_one_222))
            }

            if (winner != null) {
                Spacer(Modifier.height(12.dp))
                val picked = context.getString(R.string.game_picked_1_s, winner)
                ResultCard(text = picked,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    onCopy = { context.copyToClipboard(context.getString(R.string.game_picked_name), picked) },
                    onShare = { context.shareText(picked) })
            }

            } else {
            Spacer(Modifier.height(20.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(context.getString(R.string.game_number_of_teams), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { if (teamCount > 2) { teamCount--; teams = emptyList() } }, enabled = teamCount > 2) {
                        Icon(Icons.Filled.Remove, contentDescription = context.getString(R.string.game_fewer_teams))
                    }
                    Text(
                        text = teamCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    FilledTonalIconButton(onClick = { if (teamCount < names.size) { teamCount++; teams = emptyList() } }, enabled = teamCount < names.size) {
                        Icon(Icons.Filled.Add, contentDescription = context.getString(R.string.game_more_teams))
                    }
                }
            }
            Text(if (names.size < 2) context.getString(R.string.game_add_one_more_name_to_make_teams_you_can_already_pick_one)
                else context.getString(R.string.game_team_sizes_differ_by_at_most_one_participant),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    val shuffled = names.shuffled()
                    val groups = List(teamCount) { mutableListOf<String>() }
                    shuffled.forEachIndexed { index, name -> groups[index % teamCount].add(name) }
                    teams = groups.mapIndexed { index, group ->
                        context.getString(R.string.game_team_1_s_2_s, index + 1,
                            group.joinToString(", ")
                        )
                    }
                },
                enabled = teamCount in 2..names.size,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Groups, contentDescription = null)
                Text(context.getString(R.string.game_split_into_teams))
            }

            if (teams.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val summary = teams.joinToString("\n")
                ResultCard(
                    text = summary,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    onCopy = { context.copyToClipboard(context.getString(R.string.game_teams), summary) },
                    onShare = { context.shareText(summary) },
                )
            }
            }
        } else if(names.isEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                context.getString(R.string.game_add_one_name_to_pick_or_at_least_two_to_split_into_teams_n),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (presentationPages.isNotEmpty()) OutlinedButton(onClick = { focusManager.clearFocus(); presenting = true }, modifier = Modifier.fillMaxWidth()) {
            Text(context.getString(R.string.group_presentation_open))
        }
        ToolActions(onClear = { confirmClear = true }, clearEnabled = names.isNotEmpty() || newName.isNotEmpty())
    }
    if (editing) AlertDialog(
        onDismissRequest = { editing = false }, title = { Text(context.getString(R.string.game_participants)) },
        text = { Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(context.getString(R.string.game_add_a_name)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { addName() }, enabled = newName.isNotBlank()) {
                Icon(Icons.Filled.Add, contentDescription = context.getString(R.string.game_add_name))
            }
        }

            Spacer(Modifier.height(12.dp))
            Text(context.getString(R.string.game_participants_1_s, names.size), style = MaterialTheme.typography.titleMedium)
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
                            if(mode=="weighted") OutlinedTextField(
                                value=weights.getOrElse(index){"1"},
                                onValueChange={value -> if(value.length<=5) {
                                    weights=weights.toMutableList().also {it[index]=value}
                                    includeWeights=true;weightedWinner=null
                                    store.edit {putString("team_picker.weights",weights.joinToString("\n"));putString("team_picker.weight_names",names.joinToString(NAME_DELIMITER))}
                                }}, singleLine=true, modifier=Modifier.weight(1f),
                                label={Text(context.getString(R.string.weighted_label,index+1,name))},
                                isError=parsePickerWeight(weights.getOrElse(index){""})==null)
                            IconButton(onClick = {
                                persistNames(names.toMutableList().also { it.removeAt(index) },weights.toMutableList().also {it.removeAt(index)})
                                winner = null
                                teams = emptyList()
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = context.getString(R.string.game_remove_participant_1_s_2_s, index + 1, name))
                            }
                        }
                    }
                }
            }


        } },
        confirmButton = { TextButton(onClick = { focusManager.clearFocus(); editing = false }) { Text(context.getString(R.string.game_done)) } },
    )

}
