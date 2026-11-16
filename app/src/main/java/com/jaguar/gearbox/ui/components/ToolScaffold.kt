package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import com.jaguar.gearbox.data.ToolHelp
import com.jaguar.gearbox.data.ToolExample
import com.jaguar.gearbox.data.ToolExampleCatalog
import com.jaguar.gearbox.data.ToolHelpCatalog
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import com.jaguar.gearbox.data.Tool
import com.jaguar.gearbox.data.Tools
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.R

/**
 * Common scaffold for a tool screen: a top app bar with a back button + tool icon and a
 * scrollable, padded content column. Replaces the per-activity MaterialToolbar setup used across
 * the Java tool activities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScaffold(
    title: String,
    icon: ImageVector,
    onNavigateBack: () -> Unit,
    history: ToolHistoryBinding? = null,
    options: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var showHistory by remember { mutableStateOf(false) }
    var showSwitcher by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showPresets by remember { mutableStateOf(false) }
    var showHandoff by remember { mutableStateOf(false) }
    // Icons identify catalog tools even when screen titles are localized or shortened.
    val helpRoute = history?.route ?: Tools.all.firstOrNull { it.icon == icon }?.route
    val help = ToolHelpCatalog.forRoute(helpRoute)
    val openTool = LocalToolOpener.current
    val presetOpener = LocalToolPresetOpener.current
    val handoff = LocalToolHandoff.current
    val hasPresets = history != null && history.configuration.isNotEmpty() && presetOpener != null
    val hasHandoffs = history != null && handoff != null && com.jaguar.gearbox.data.compatibleToolHandoffs(history.route, history.inputs, history.output).isNotEmpty()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.layout_back))
                    }
                },
                actions = {
                    if (history != null) IconButton(onClick = { showHistory = true }) { Icon(Icons.Filled.History, "History") }
                    if (help != null || openTool != null || hasPresets || hasHandoffs) Box {
                        IconButton(onClick = { showMore = true }) {
                            Icon(Icons.Filled.MoreVert, "More tool actions")
                        }
                        DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                            if (hasPresets) DropdownMenuItem(text = { Text("Presets") }, onClick = {
                                showMore = false; showPresets = true
                            })
                            if (hasHandoffs) DropdownMenuItem(text = { Text("Use result in…") }, onClick = {
                                showMore = false; showHandoff = true
                            })
                            if (help != null) DropdownMenuItem(text = { Text("Help & examples") }, onClick = {
                                showMore = false
                                showHelp = true
                            })
                            if (openTool != null) DropdownMenuItem(text = { Text("Switch tool") }, onClick = {
                                showMore = false
                                showSwitcher = true
                            }, leadingIcon = { Icon(Icons.Filled.Search, null) })
                        }
                    }
                    options?.invoke()
                },
                colors = TopAppBarDefaults.topAppBarColors(),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                content()
            }
        }
    }
    if (showPresets && history != null) ToolPresetsDialog(history) { showPresets = false }
    if (showHandoff && history != null) ToolHandoffDialog(history) { showHandoff = false }
    if (showHelp && help != null) ToolHelpDialog(
        title = title,
        help = help,
        example = if (handoff != null) ToolExampleCatalog.forRoute(helpRoute) else null,
        onLoadExample = { example ->
            helpRoute?.let { handoff?.invoke(it, example.inputs) }
            showHelp = false
        },
        onDismiss = { showHelp = false },
    )
    if (showHistory && history != null) ToolHistoryDialog(history) { showHistory = false }
    if (showSwitcher && openTool != null) ToolSwitcher(onOpen = {
        showSwitcher = false
        openTool(it)
    }, onDismiss = { showSwitcher = false })
}

val LocalToolOpener = staticCompositionLocalOf<((Tool) -> Unit)?> { null }

@Composable
internal fun ToolSwitcher(onOpen: (Tool) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val recents = remember(context) { com.jaguar.gearbox.data.RecentToolsStore(context) }
    val recent = if (query.isBlank() && recents.showRecentTools)
        recents.recent.mapNotNull(Tools::byRoute) else emptyList()
    val matches = Tools.all.filter { tool ->
        query.isBlank() || (listOf(tool.name, tool.description, tool.category.label) + tool.keywords).any { it.contains(query.trim(), true) }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Switch tool") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Switching keeps non-password drafts in this navigation session. Back leaves the current tool.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Find a tool") }, singleLine = true)
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                if (recent.isNotEmpty()) {
                    item { Text("Recent tools", style = MaterialTheme.typography.titleSmall) }
                    items(recent, key = { "recent:${it.route}" }) { tool ->
                        ListItem(headlineContent = { Text(tool.name) }, supportingContent = { Text(tool.category.label) },
                            leadingContent = { Icon(tool.icon, null) }, modifier = Modifier.clickable { onOpen(tool) })
                    }
                    item { Text("All other tools", style = MaterialTheme.typography.titleSmall) }
                }
                items(matches.filterNot { candidate -> recent.any { it.route == candidate.route } }, key = { it.route }) { tool ->
                    ListItem(headlineContent = { Text(tool.name) }, supportingContent = { Text(tool.category.label) },
                        leadingContent = { Icon(tool.icon, null) }, modifier = Modifier.clickable { onOpen(tool) })
                }
                if (matches.isEmpty()) item { Text("No tools match. Try another name or task.") }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

/** Scrolling prose stays usable with large fonts and on short landscape screens. */
@Composable
private fun ToolHelpDialog(
    title: String,
    help: ToolHelp,
    example: ToolExample?,
    onLoadExample: (ToolExample) -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmReplacement by remember { mutableStateOf(false) }
    if (confirmReplacement && example != null) {
        AlertDialog(
            onDismissRequest = { confirmReplacement = false },
            title = { Text("Replace current inputs?") },
            text = { Text("This replaces the current tool inputs with the example. Calculations may update automatically; QR and hash still need Generate. This action does not save a copy of your current inputs.", modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = {
                confirmReplacement = false
                onLoadExample(example)
            }) { Text("Replace inputs") } },
            dismissButton = { TextButton(onClick = { confirmReplacement = false }) { Text("Cancel") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Help: $title") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HelpSection("How to use", help.instructions)
                HelpSection("Example", example?.description ?: help.example)
                Text(if (example == null) "Examples are reference only; your inputs stay unchanged." else "Use example loads these inputs only after you confirm.", style = MaterialTheme.typography.bodySmall)
                HelpSection("Keep in mind", help.limitation)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = { if (example != null) TextButton(onClick = { confirmReplacement = true }) { Text("Use example") } },
    )
}

@Composable
private fun HelpSection(label: String, body: String) {
    Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
    Text(body, style = MaterialTheme.typography.bodyMedium)
}
