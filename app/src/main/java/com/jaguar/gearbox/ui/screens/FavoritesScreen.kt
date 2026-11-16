package com.jaguar.gearbox.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jaguar.gearbox.data.FavoritesStore
import com.jaguar.gearbox.data.Tool
import kotlinx.coroutines.launch

/** A personal launcher first; editing is a deliberate, accessible mode. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(tools: List<Tool>, favorites: FavoritesStore, onOpenTool: (Tool) -> Unit) {
    val toolsByRoute = remember(tools) { tools.associateBy { it.route } }
    val favTools = favorites.favorites.mapNotNull { toolsByRoute[it] }
    var editing by rememberSaveable { mutableStateOf(false) }
    var choosing by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var draggedRoute by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    fun finishDrag() { draggedRoute = null; dragOffset = 0f }

    BackHandler(enabled = editing && !choosing) { finishDrag(); editing = false }
    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0), snackbarHost = { SnackbarHost(snackbar) }) { insets ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "favorites_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your toolbox", style = MaterialTheme.typography.headlineMedium)
                    Text("Your everyday tools, one tap away.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { choosing = true }) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Choose tools")
                        }
                        if (favTools.isNotEmpty()) TextButton(onClick = { finishDrag(); editing = !editing }) {
                            Text(if (editing) "Done" else "Edit")
                        }
                    }
                    if (editing) Text("Drag a handle or use the arrows to reorder. Remove a tool with the minus button.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            if (favTools.isEmpty()) item(key = "favorites_empty") {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.StarOutline, null, Modifier.size(32.dp))
                        Text("Make this space yours", style = MaterialTheme.typography.titleLarge)
                        Text("Choose the tools you reach for most. Their order is always yours to decide.")
                    }
                }
            }
            itemsIndexed(favTools, key = { _, tool -> tool.route }) { index, tool ->
                val isDragged = tool.route == draggedRoute
                fun moveBy(delta: Int): Boolean {
                    val target = favTools.getOrNull(index + delta) ?: return false
                    finishDrag()
                    favorites.move(favorites.favorites.indexOf(tool.route), favorites.favorites.indexOf(target.route))
                    return true
                }
                val moveUp = "Move ${tool.name} up"
                val moveDown = "Move ${tool.name} down"
                Card(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
                        .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                        .zIndex(if (isDragged) 1f else 0f)
                        .then(if (!editing) Modifier.clickable { onOpenTool(tool) } else Modifier)
                        .semantics {
                            stateDescription = "${index + 1} of ${favTools.size}"
                            if (editing) customActions = buildList {
                                if (index > 0) add(CustomAccessibilityAction(moveUp) { moveBy(-1) })
                                if (index < favTools.lastIndex) add(CustomAccessibilityAction(moveDown) { moveBy(1) })
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(if (isDragged) 8.dp else 0.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(tool.icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(tool.name, style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(tool.category.label, style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (editing) IconButton(onClick = {
                                finishDrag()
                                val position = favorites.favorites.indexOf(tool.route)
                                favorites.toggle(tool.route)
                                scope.launch {
                                    snackbar.currentSnackbarData?.dismiss()
                                    if (snackbar.showSnackbar("${tool.name} removed", "Undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed)
                                        favorites.restore(tool.route, position)
                                }
                            }) { Icon(Icons.Default.RemoveCircleOutline, "Remove ${tool.name}") }
                        }
                        if (editing) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { moveBy(-1) }, enabled = index > 0) { Icon(Icons.Default.ArrowUpward, moveUp) }
                            IconButton(onClick = { moveBy(1) }, enabled = index < favTools.lastIndex) { Icon(Icons.Default.ArrowDownward, moveDown) }
                            Icon(Icons.Default.DragHandle, "Drag to reorder ${tool.name}",
                                modifier = Modifier.size(48.dp).pointerInput(tool.route) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggedRoute = tool.route; dragOffset = 0f },
                                        onDragEnd = { finishDrag() }, onDragCancel = { finishDrag() },
                                        onDrag = { change, delta ->
                                            change.consume(); dragOffset += delta.y
                                            val route = draggedRoute ?: return@detectDragGesturesAfterLongPress
                                            val layout = listState.layoutInfo
                                            val current = layout.visibleItemsInfo.firstOrNull { it.key == route }
                                                ?: return@detectDragGesturesAfterLongPress
                                            val center = current.offset + dragOffset + current.size / 2f
                                            val edge = 64.dp.toPx()
                                            val scroll = when {
                                                center > layout.viewportEndOffset - edge -> 18f
                                                center < layout.viewportStartOffset + edge -> -18f
                                                else -> 0f
                                            }
                                            if (scroll != 0f) scope.launch { listState.scrollBy(scroll) }
                                            val target = layout.visibleItemsInfo.firstOrNull {
                                                it.key != route && it.key in favorites.favorites &&
                                                    center >= it.offset && center < it.offset + it.size
                                            } ?: return@detectDragGesturesAfterLongPress
                                            favorites.move(favorites.favorites.indexOf(route), favorites.favorites.indexOf(target.key as String))
                                            dragOffset -= target.offset - current.offset
                                        },
                                    )
                                })
                        }
                    }
                }
            }
        }
    }
    if (choosing) ChooseFavoriteTools(tools, favorites, onDismiss = { choosing = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChooseFavoriteTools(tools: List<Tool>, favorites: FavoritesStore, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val matches = tools.filter { tool ->
        query.isBlank() || tool.name.contains(query, true) || tool.description.contains(query, true) ||
            tool.keywords.any { it.contains(query, true) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Choose tools", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Search tools") }, leadingIcon = { Icon(Icons.Default.Search, null) })
            LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(max = 420.dp)) {
                if (matches.isEmpty()) item { Text("No matching tools", Modifier.padding(16.dp)) }
                items(matches, key = { it.route }) { tool ->
                    val existing = favorites.isFavorite(tool.route)
                    val checked = existing || tool.route in selected
                    Row(Modifier.fillMaxWidth().toggleable(checked, enabled = !existing, role = Role.Checkbox) {
                        selected = if (it) selected + tool.route else selected - tool.route
                    }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked, onCheckedChange = null, enabled = !existing)
                        Icon(tool.icon, null, Modifier.padding(horizontal = 8.dp).size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tool.name)
                            Text(if (existing) "Already in Favorites" else tool.category.label,
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Button(enabled = selected.isNotEmpty(), onClick = {
                    favorites.addAll(tools.filter { it.route in selected }.map { it.route }); onDismiss()
                }) { Text("Add ${selected.size}") }
            }
        }
    }
}
