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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jaguar.gearbox.data.FavoritesStore
import com.jaguar.gearbox.data.Tool
import kotlin.math.roundToInt

private val ROW_HEIGHT = 72.dp
private val ROW_SPACING = 8.dp

/**
 * Favourites screen: shows only the tools the user has starred, as a single-column list they can
 * long-press-and-drag to reorder, since this is meant to be their personal quick-access list.
 */
@Composable
fun FavoritesScreen(
    tools: List<Tool>,
    favorites: FavoritesStore,
    onOpenTool: (Tool) -> Unit,
) {
    val toolsByRoute = remember(tools) { tools.associateBy { it.route } }
    val favTools = favorites.favorites.mapNotNull { toolsByRoute[it] }

    if (favTools.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No favourites yet.\nTap the star on a tool to add it here.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { (ROW_HEIGHT + ROW_SPACING).toPx() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
    ) {
        itemsIndexed(favTools, key = { _, tool -> tool.route }) { index, tool ->
            val isDragged = index == draggedIndex
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT)
                    .graphicsLayer { translationY = if (isDragged) dragOffset else 0f }
                    .zIndex(if (isDragged) 1f else 0f)
                    .clickable(onClick = { onOpenTool(tool) }),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDragged) 8.dp else 2.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    ) {
                        Text(
                            text = tool.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = tool.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { favorites.toggle(tool.route) }) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Remove ${tool.name} from favourites",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = "Drag to reorder ${tool.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .width(32.dp)
                            .pointerInput(tool.route) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedIndex = index
                                        dragOffset = 0f
                                    },
                                    onDragEnd = {
                                        draggedIndex = null
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggedIndex = null
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, delta ->
                                        change.consume()
                                        dragOffset += delta.y
                                        val current =
                                            draggedIndex ?: return@detectDragGesturesAfterLongPress
                                        val target =
                                            (current + (dragOffset / rowHeightPx).roundToInt())
                                                .coerceIn(0, favTools.lastIndex)
                                        if (target != current) {
                                            favorites.move(current, target)
                                            dragOffset -= (target - current) * rowHeightPx
                                            draggedIndex = target
                                        }
                                    },
                                )
                            },
                    )
                }
            }
        }
    }
}
