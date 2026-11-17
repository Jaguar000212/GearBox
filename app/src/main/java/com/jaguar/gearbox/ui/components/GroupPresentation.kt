package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jaguar.gearbox.R

/** Presentation only. Caller must omit the source UI while shown, including its semantics. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupPresentation(pages: List<String>, onClose: () -> Unit) {
    if (pages.isEmpty()) return
    val context = LocalContext.current
    // Do not restore a revealed page after recreation or after the source result changes.
    var page by remember(pages) { mutableIntStateOf(0) }
    var revealed by remember(pages) { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(context.getString(R.string.group_presentation_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = onClose) { Text(context.getString(R.string.group_presentation_close)) }
                }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(context.getString(R.string.group_presentation_instructions), textAlign = TextAlign.Center)
                    Text(context.getString(R.string.group_presentation_page, page + 1, pages.size), style = MaterialTheme.typography.titleMedium)
                    // Hidden data is not composed at all; it cannot be discovered by TalkBack,
                    // text selection, testing semantics or a hidden off-screen text node.
                    if (revealed) Text(pages[page], style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite })
                    else Text(context.getString(R.string.group_presentation_hidden), style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    Button(onClick = { revealed = !revealed }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text(context.getString(if (revealed) R.string.group_presentation_hide else R.string.group_presentation_reveal))
                    }
                    if (pages.size > 1) FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = page > 0, onClick = { page--; revealed = false }) {
                            Text(context.getString(R.string.group_presentation_previous))
                        }
                        OutlinedButton(enabled = page < pages.lastIndex, onClick = { page++; revealed = false }) {
                            Text(context.getString(R.string.group_presentation_next))
                        }
                    }
                    Text(context.getString(R.string.group_presentation_unchanged), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
