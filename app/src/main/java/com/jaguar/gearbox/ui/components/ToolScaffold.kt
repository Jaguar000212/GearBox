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
    val openTool = LocalToolOpener.current
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
                    if (openTool != null) IconButton(onClick = { showSwitcher = true }) { Icon(Icons.Filled.Search, "Switch tool") }
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
    if (showHistory && history != null) ToolHistoryDialog(history) { showHistory = false }
    if (showSwitcher && openTool != null) ToolSwitcher(onOpen = {
        showSwitcher = false
        openTool(it)
    }, onDismiss = { showSwitcher = false })
}

val LocalToolOpener = staticCompositionLocalOf<((Tool) -> Unit)?> { null }

@Composable
private fun ToolSwitcher(onOpen: (Tool) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val matches = Tools.all.filter { tool ->
        query.isBlank() || (listOf(tool.name, tool.description) + tool.keywords).any { it.contains(query.trim(), true) }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Switch tool") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Find a tool") }, singleLine = true)
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(matches, key = { it.route }) { tool ->
                    ListItem(headlineContent = { Text(tool.name) }, supportingContent = { Text(tool.category.label) },
                        leadingContent = { Icon(tool.icon, null) }, modifier = Modifier.clickable { onOpen(tool) })
                }
                if (matches.isEmpty()) item { Text("No tools match. Try another name or task.") }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
