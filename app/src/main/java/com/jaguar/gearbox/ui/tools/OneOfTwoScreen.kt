package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.R

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.RandomResultSnapshots
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory
import kotlin.random.Random

@Composable
fun OneOfTwoScreen(onNavigateBack: () -> Unit) {
    OneOfTwoScreen(chooseIndex = { Random.nextInt(2) }, onNavigateBack = onNavigateBack)
}

@Composable
internal fun OneOfTwoScreen(
    chooseIndex: () -> Int,
    onNavigateBack: () -> Unit,
) {
    var runId by rememberSaveable { mutableStateOf("") }
    var optionA by rememberSaveable { mutableStateOf("") }
    var optionB by rememberSaveable { mutableStateOf("") }
    // Keep the selected slot, not just its label: identical labels are still two choices.
    var chosenIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val summary = chosenIndex?.let { index ->
        context.getString(R.string.game_option_1_s_selected_n_2_s, index + 1, if (index == 0) optionA.trim() else optionB.trim())
    }

    val history = rememberToolHistory(
        route = Tools.ROUTE_ONE_OF_TWO,
        inputs = mapOf("optionA" to optionA, "optionB" to optionB, "_run" to runId, "_choice" to (chosenIndex?.toString() ?: "")),
        output = summary,
        recordingDelayMillis = 0L,
        onRestore = { saved ->
            optionA = saved["optionA"].orEmpty(); optionB = saved["optionB"].orEmpty()
            chosenIndex = RandomResultSnapshots.choice(saved)
            runId = saved["_run"].orEmpty()
        },
    )

    ToolScaffold(
        title = context.getString(R.string.game_1_of_2),
        icon = Tools.byRoute(Tools.ROUTE_ONE_OF_TWO)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        Text(context.getString(R.string.game_two_choices_one_decision), style = MaterialTheme.typography.titleLarge)
        Text(context.getString(R.string.game_each_option_has_an_equal_chance_edit_either_option_to_star),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = optionA,
            onValueChange = { optionA = it; chosenIndex = null },
            label = { Text(context.getString(R.string.game_option_1)) },
            minLines = 1, maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = optionB,
            onValueChange = { optionB = it; chosenIndex = null },
            label = { Text(context.getString(R.string.game_option_2)) },
            minLines = 1, maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        if (optionA.isNotBlank() && optionA.trim() == optionB.trim()) {
            Spacer(Modifier.height(8.dp))
            Text(context.getString(R.string.game_both_labels_are_the_same_the_selected_option_number_will_d),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { focusManager.clearFocus(); runId = java.util.UUID.randomUUID().toString(); chosenIndex = chooseIndex() },
            enabled = optionA.isNotBlank() && optionB.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Shuffle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (chosenIndex == null) context.getString(R.string.game_pick_one) else context.getString(R.string.game_pick_again))
        }
        Spacer(Modifier.height(20.dp))
        if (summary != null) {
            ResultCard(text = summary, textStyle = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        } else {
            Text(context.getString(R.string.game_enter_both_options_then_pick_one), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        ToolActions(
            onCopy = { summary?.let { context.copyToClipboard(context.getString(R.string.game_1_of_2), it) } },
            onShare = { summary?.let { context.shareText(it) } },
            resultEnabled = summary != null,
            onClear = { optionA = ""; optionB = ""; chosenIndex = null },
            clearEnabled = optionA.isNotEmpty() || optionB.isNotEmpty(),
        )
    }
}
