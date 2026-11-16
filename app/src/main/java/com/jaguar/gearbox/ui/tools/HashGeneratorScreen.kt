package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.TextHashAlgorithm
import com.jaguar.gearbox.logic.textHashHex
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.SensitiveTextShareDialog
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.CompactChoiceSelector
import com.jaguar.gearbox.ui.components.rememberToolHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HashGeneratorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var algorithm by rememberSaveable { mutableStateOf(TextHashAlgorithm.SHA256.name) }
    val selectedAlgorithm = TextHashAlgorithm.entries.firstOrNull { it.name == algorithm } ?: TextHashAlgorithm.SHA256
    var shareReview by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var restoredInput by rememberSaveable { mutableStateOf(false) }
    // A new state holder for each request hides the old result immediately, before effects run.
    var hash by remember(input, algorithm, restoredInput) { mutableStateOf<String?>(null) }
    LaunchedEffect(input, algorithm, restoredInput) {
        if (input.isNotEmpty() && !restoredInput) {
            hash = withContext(Dispatchers.Default) { textHashHex(input, selectedAlgorithm) }
        }
    }

    val history = rememberToolHistory(
        route = Tools.ROUTE_HASH_GENERATOR,
        inputs = mapOf("text" to input, "algorithm" to selectedAlgorithm.name), output = hash,
        resultRequiresExplicitAction = true,
        onRestore = { values ->
            input = values["text"].orEmpty()
            algorithm = TextHashAlgorithm.entries.firstOrNull { it.name == values["algorithm"] }?.name ?: TextHashAlgorithm.SHA256.name
            restoredInput = true
            hash = null
        },
        modeInputs = mapOf("algorithm" to selectedAlgorithm.name),
        onRestoreMode = { values ->
            algorithm = TextHashAlgorithm.entries.firstOrNull { it.name == values["algorithm"] }?.name ?: TextHashAlgorithm.SHA256.name
        },
    )
    ToolScaffold(title = stringResource(R.string.hash_title),
        icon = Tools.byRoute(Tools.ROUTE_HASH_GENERATOR)!!.icon, onNavigateBack = onNavigateBack, history = history) {
        OutlinedTextField(value = input, onValueChange = { input = it; restoredInput = false },
            label = { Text(stringResource(R.string.hash_input)) },
            supportingText = { Text(stringResource(R.string.hash_exact_input)) },
            minLines = 3, maxLines = 8, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        CompactChoiceSelector(
            label = context.getString(R.string.special_algorithm), selected = selectedAlgorithm, choices = TextHashAlgorithm.entries,
            choiceLabel = { it.label }, onSelect = { algorithm = it.name; restoredInput = false },
        )
        Text(stringResource(if (selectedAlgorithm.legacy) R.string.hash_legacy else R.string.hash_help),
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        if (restoredInput && input.isNotEmpty()) {
            Text(context.getString(R.string.special_inputs_restored_generate_a_fresh_result_when_you_are_ready))
            Button(onClick = { restoredInput = false }) { Text(context.getString(R.string.special_generate_hash)) }
        }
        val currentHash = hash
        if (currentHash != null) {
            Text(stringResource(R.string.hash_digest_title, selectedAlgorithm.label),
                style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.hash_length, currentHash.length),
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            ResultCard(text = currentHash, selectable = false,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace))
        } else if (!restoredInput) {
            Text(stringResource(if (input.isEmpty()) R.string.hash_empty else R.string.hash_working))
        }
        Spacer(Modifier.height(12.dp))
        ToolActions(
            onCopy = { hash?.let { context.copyToClipboard(selectedAlgorithm.label, it, sensitive = true) } },
            onShare = { hash?.let { shareReview = Triple(input, algorithm, it) } },
            onClear = { input = ""; restoredInput = false }, resultEnabled = hash != null, clearEnabled = input.isNotEmpty())
    }
    shareReview?.let { snapshot ->
        if (snapshot.first != input || snapshot.second != algorithm || snapshot.third != hash || restoredInput) {
            LaunchedEffect(snapshot) { shareReview = null }
        } else SensitiveTextShareDialog(snapshot.third,
            onConfirm = {
                shareReview = null
                if (snapshot.first == input && snapshot.second == algorithm && snapshot.third == hash && !restoredInput)
                    context.shareText(snapshot.third)
            }, onDismiss = { shareReview = null })
    }
}
