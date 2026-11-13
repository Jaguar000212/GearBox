package com.jaguar.gearbox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.FavoritesStore
import com.jaguar.gearbox.data.Tool

/** Reorder using visible buttons, accessibility actions, or a long-press drag handle. */
@Composable
fun FavoritesScreen(
    tools: List<Tool>,
    favorites: FavoritesStore,
    onOpenTool: (Tool) -> Unit,
) {
    val toolsByRoute = remember(tools) { tools.associateBy { it.route } }
    val favTools = favorites.favorites.mapNotNull { toolsByRoute[it] }
    if (favTools.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.favorites_empty), style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center)
        }
        return
    }

    val listState = rememberLazyListState()
    var draggedRoute by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    fun finishDrag() {
        draggedRoute = null
        dragOffset = 0f
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "favorites_reorder_hint") {
            Text(stringResource(R.string.favorites_reorder_hint), style = MaterialTheme.typography.bodySmall)
        }
        itemsIndexed(favTools, key = { _, tool -> tool.route }) { index, tool ->
            val isDragged = tool.route == draggedRoute
            val moveUpLabel = stringResource(R.string.favorites_move_up, tool.name)
            val moveDownLabel = stringResource(R.string.favorites_move_down, tool.name)
            val positionLabel = stringResource(R.string.favorites_position, index + 1, favTools.size)
            fun moveBy(delta: Int): Boolean {
                val target = favTools.getOrNull(index + delta) ?: return false
                finishDrag()
                // Map routes to persisted positions; unknown/removed tools may still be stored.
                favorites.move(favorites.favorites.indexOf(tool.route), favorites.favorites.indexOf(target.route))
                return true
            }
            Card(
                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
                    .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                    .zIndex(if (isDragged) 1f else 0f)
                    .clickable { onOpenTool(tool) }
                    .semantics {
                        stateDescription = positionLabel
                        customActions = buildList {
                            if (index > 0) add(CustomAccessibilityAction(moveUpLabel) { moveBy(-1) })
                            if (index < favTools.lastIndex) add(CustomAccessibilityAction(moveDownLabel) { moveBy(1) })
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDragged) 8.dp else 2.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(tool.icon, contentDescription = null, modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(tool.name, style = MaterialTheme.typography.titleMedium,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(tool.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { finishDrag(); favorites.toggle(tool.route) }) {
                            Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.favorites_remove, tool.name),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { moveBy(-1) }, enabled = index > 0) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = moveUpLabel)
                        }
                        IconButton(onClick = { moveBy(1) }, enabled = index < favTools.lastIndex) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = moveDownLabel)
                        }
                        Icon(
                            Icons.Filled.DragHandle,
                            contentDescription = stringResource(R.string.favorites_drag, tool.name),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                                .pointerInput(tool.route) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggedRoute = tool.route; dragOffset = 0f },
                                        onDragEnd = { finishDrag() },
                                        onDragCancel = { finishDrag() },
                                        onDrag = { change, delta ->
                                            change.consume()
                                            dragOffset += delta.y
                                            val route = draggedRoute ?: return@detectDragGesturesAfterLongPress
                                            val visible = listState.layoutInfo.visibleItemsInfo
                                            val current = visible.firstOrNull { it.key == route }
                                                ?: return@detectDragGesturesAfterLongPress
                                            val center = current.offset + dragOffset + current.size / 2f
                                            val target = visible.firstOrNull {
                                                it.key != route && favorites.favorites.any { routeKey -> routeKey == it.key } &&
                                                    center >= it.offset && center < it.offset + it.size
                                            } ?: return@detectDragGesturesAfterLongPress
                                            favorites.move(favorites.favorites.indexOf(route),
                                                favorites.favorites.indexOf(target.key as String))
                                            dragOffset -= target.offset - current.offset
                                        },
                                    )
                                },
                        )
                    }
                }
            }
        }
    }
}
