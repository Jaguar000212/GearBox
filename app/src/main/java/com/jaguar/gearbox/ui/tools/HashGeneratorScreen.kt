package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
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
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HashGeneratorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var algorithm by rememberSaveable { mutableStateOf(TextHashAlgorithm.SHA256.name) }
    val selectedAlgorithm = TextHashAlgorithm.valueOf(algorithm)
    // A new state holder for each request hides the old result immediately, before effects run.
    var hash by remember(input, algorithm) { mutableStateOf<String?>(null) }
    LaunchedEffect(input, algorithm) {
        if (input.isNotEmpty()) {
            hash = withContext(Dispatchers.Default) { textHashHex(input, selectedAlgorithm) }
        }
    }

    ToolScaffold(title = stringResource(R.string.hash_title),
        icon = Tools.byRoute(Tools.ROUTE_HASH_GENERATOR)!!.icon, onNavigateBack = onNavigateBack) {
        OutlinedTextField(value = input, onValueChange = { input = it },
            label = { Text(stringResource(R.string.hash_input)) },
            supportingText = { Text(stringResource(R.string.hash_exact_input)) },
            minLines = 3, maxLines = 8, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextHashAlgorithm.entries.forEach { entry ->
                FilterChip(selected = selectedAlgorithm == entry, onClick = { algorithm = entry.name },
                    label = { Text(entry.label) })
            }
        }
        Text(stringResource(if (selectedAlgorithm.legacy) R.string.hash_legacy else R.string.hash_help),
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(20.dp))
        val currentHash = hash
        if (currentHash != null) {
            Text(stringResource(R.string.hash_digest_title, selectedAlgorithm.label),
                style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.hash_length, currentHash.length),
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            SelectionContainer {
                ResultCard(text = currentHash,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace))
            }
        } else {
            Text(stringResource(if (input.isEmpty()) R.string.hash_empty else R.string.hash_working))
        }
        Spacer(Modifier.height(12.dp))
        ToolActions(
            onCopy = { hash?.let { context.copyToClipboard(selectedAlgorithm.label, it) } },
            onShare = { hash?.let { context.shareText(it) } },
            onClear = { input = "" }, resultEnabled = hash != null, clearEnabled = input.isNotEmpty())
    }
}
