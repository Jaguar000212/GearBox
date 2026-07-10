package com.jaguar.gearbox.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.FavoritesStore
import com.jaguar.gearbox.data.Tool
import com.jaguar.gearbox.ui.components.ToolCard

/** Home screen: a 2-column grid of every available tool (Compose port of the `AllTools` fragment). */
@Composable
fun HomeScreen(
    tools: List<Tool>,
    favorites: FavoritesStore,
    onOpenTool: (Tool) -> Unit,
    onShowDescription: (Tool) -> Unit,
) {
    ToolGrid(
        tools = tools,
        favorites = favorites,
        onOpenTool = onOpenTool,
        onShowDescription = onShowDescription,
        emptyMessage = "No tools available.",
    )
}

/** Shared grid used by both the home and favourites screens. */
@Composable
fun ToolGrid(
    tools: List<Tool>,
    favorites: FavoritesStore,
    onOpenTool: (Tool) -> Unit,
    onShowDescription: (Tool) -> Unit,
    emptyMessage: String,
) {
    if (tools.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(tools, key = { it.route }) { tool ->
            ToolCard(
                tool = tool,
                isFavorite = favorites.isFavorite(tool.route),
                onClick = { onOpenTool(tool) },
                onLongClick = { onShowDescription(tool) },
                onToggleFavorite = { favorites.toggle(tool.route) },
            )
        }
    }
}
