package com.jaguar.gearbox.ui.screens

import androidx.compose.runtime.Composable
import com.jaguar.gearbox.data.FavoritesStore
import com.jaguar.gearbox.data.Tool

/** Favourites screen: shows only the tools the user has starred. */
@Composable
fun FavoritesScreen(
    tools: List<Tool>,
    favorites: FavoritesStore,
    onOpenTool: (Tool) -> Unit,
    onShowDescription: (Tool) -> Unit,
) {
    val favTools = tools.filter { favorites.isFavorite(it.route) }
    ToolGrid(
        tools = favTools,
        favorites = favorites,
        onOpenTool = onOpenTool,
        onShowDescription = onShowDescription,
        emptyMessage = "No favourites yet.\nTap the star on a tool to add it here.",
    )
}
