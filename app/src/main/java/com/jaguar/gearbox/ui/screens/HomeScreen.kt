package com.jaguar.gearbox.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.FavoritesStore
import com.jaguar.gearbox.data.RecentToolsStore
import com.jaguar.gearbox.data.Tool
import com.jaguar.gearbox.data.ToolCategory
import com.jaguar.gearbox.logic.adaptiveToolColumnCount
import com.jaguar.gearbox.ui.components.ToolCard

/**
 * Home screen: a searchable, category-grouped view of every available tool (Compose port of the
 * `AllTools` fragment, extended with search and grouping since the tool count has grown).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    tools: List<Tool>,
    favorites: FavoritesStore,
    recentTools: RecentToolsStore,
    onOpenTool: (Tool) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    val filtered = tools.filter { tool ->
        val matchesQuery = query.isBlank() ||
                tool.name.contains(query, ignoreCase = true) ||
                tool.description.contains(query, ignoreCase = true) ||
                tool.keywords.any { it.contains(query, ignoreCase = true) }
        val matchesCategory = selectedCategory == null || tool.category.name == selectedCategory
        matchesQuery && matchesCategory
    }
    val grouped = ToolCategory.entries.mapNotNull { category ->
        val toolsInCategory = filtered.filter { it.category == category }
        if (toolsInCategory.isEmpty()) null else category to toolsInCategory
    }
    val recentToolObjs =
        recentTools.recent.mapNotNull { route -> tools.firstOrNull { it.route == route } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = adaptiveToolColumnCount(maxWidth.value, LocalDensity.current.fontScale)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (query.isBlank() && recentToolObjs.isNotEmpty()) {
                item {
                    RecentToolsRow(tools = recentToolObjs, onOpenTool = onOpenTool)
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.layout_search_tools)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.layout_clear_search))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(stringResource(R.string.layout_all_categories)) },
                        )
                    }
                    items(ToolCategory.entries) { category ->
                        FilterChip(
                            selected = selectedCategory == category.name,
                            onClick = {
                                selectedCategory =
                                    if (selectedCategory == category.name) null else category.name
                            },
                            label = { Text(category.label) },
                        )
                    }
                }
            }

            if (grouped.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.layout_no_tools_found), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            grouped.forEach { (category, toolsInCategory) ->
                item {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 8.dp).semantics { heading() },
                    )
                }
                items(toolsInCategory.chunked(columns)) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { tool ->
                            ToolCard(
                                tool = tool,
                                isFavorite = favorites.isFavorite(tool.route),
                                onClick = { onOpenTool(tool) },
                                onToggleFavorite = { favorites.toggle(tool.route) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                        repeat(columns - row.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/** Quick-access row for the last few opened tools, shown above search while it's empty. */
@Composable
private fun RecentToolsRow(tools: List<Tool>, onOpenTool: (Tool) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.layout_recent_tools),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp).semantics { heading() },
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(tools, key = { it.route }) { tool ->
                AssistChip(
                    onClick = { onOpenTool(tool) },
                    label = { Text(tool.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
        }
    }
}

/** Shared grid used by the favourites screen (ungrouped, since favourites are usually few). */
@Composable
fun ToolGrid(
    tools: List<Tool>,
    favorites: FavoritesStore,
    onOpenTool: (Tool) -> Unit,
    emptyMessage: String,
) {
    if (tools.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyMessage, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val columns = adaptiveToolColumnCount(maxWidth.value, LocalDensity.current.fontScale)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
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
                    onToggleFavorite = { favorites.toggle(tool.route) },
                )
            }
        }
    }
}
