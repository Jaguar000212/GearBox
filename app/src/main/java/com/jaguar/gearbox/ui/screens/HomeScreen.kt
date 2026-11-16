package com.jaguar.gearbox.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.*
import com.jaguar.gearbox.logic.adaptiveToolColumnCount
import com.jaguar.gearbox.ui.components.ToolCard

/** Discovery stays separate from the user's curated Favorites. */
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
    var showCategories by rememberSaveable { mutableStateOf(false) }
    val activeCategory = ToolCategory.entries.firstOrNull { it.name == selectedCategory }
    val search = query.trim()
    val filtered = tools.filter { tool ->
        (activeCategory == null || tool.category == activeCategory) &&
            (search.isEmpty() || tool.name.contains(search, true) ||
                tool.description.contains(search, true) || tool.category.label.contains(search, true) ||
                tool.keywords.any { it.contains(search, true) })
    }
    val recentToolObjs = recentTools.recent.mapNotNull { route -> tools.firstOrNull { it.route == route } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Your everyday toolbox", style = MaterialTheme.typography.headlineSmall)
                Text("Find a tool. Make something easier.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text(stringResource(R.string.layout_search_tools)) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, stringResource(R.string.layout_clear_search))
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                AssistChip(onClick = { showCategories = true },
                    label = { Text(activeCategory?.label ?: "All tools") },
                    trailingIcon = { Icon(Icons.Default.ExpandMore, null) })
                Text(pluralStringResource(R.plurals.home_tool_count, filtered.size, filtered.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (recentTools.showRecentTools && search.isEmpty() && activeCategory == null && recentToolObjs.isNotEmpty()) {
            item(key = "recent-tools") { RecentToolsRow(recentToolObjs, onOpenTool) }
        }
        if (filtered.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Search, null, Modifier.size(32.dp))
                    Text(stringResource(R.string.layout_no_tools_found), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.home_empty_hint), style = MaterialTheme.typography.bodyMedium)
                    if (activeCategory != null) TextButton(onClick = { selectedCategory = null }) {
                        Text("Search all tools")
                    }
                    TextButton(onClick = { query = ""; selectedCategory = null }) {
                        Text(stringResource(R.string.home_reset_filters))
                    }
                }
            }
        } else if (search.isNotEmpty()) {
            // Search is one flat list, never repeated category groups.
            items(filtered, key = { it.route }) { tool ->
                DiscoveryToolRow(tool, favorites, onOpenTool, showCategory = true)
            }
        } else {
            ToolCategory.entries.forEach { category ->
                val members = filtered.filter { it.category == category }
                if (members.isNotEmpty()) {
                    item(key = "category-${category.name}") {
                        Text(category.label, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp)
                                .semantics { heading() })
                    }
                    items(members, key = { it.route }) { tool ->
                        DiscoveryToolRow(tool, favorites, onOpenTool, showCategory = false)
                    }
                }
            }
        }
    }
    if (showCategories) {
        ModalBottomSheet(onDismissRequest = { showCategories = false }) {
            Text("Browse categories", style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    ListItem(headlineContent = { Text("All tools") },
                        supportingContent = { Text("${tools.size} tools") },
                        trailingContent = { RadioButton(activeCategory == null, onClick = null) },
                        modifier = Modifier.selectable(selected = activeCategory == null, role = Role.RadioButton) { selectedCategory = null; showCategories = false })
                }
                items(ToolCategory.entries, key = { it.name }) { category ->
                    ListItem(headlineContent = { Text(category.label) },
                        supportingContent = { Text("${tools.count { it.category == category }} tools") },
                        trailingContent = { RadioButton(activeCategory == category, onClick = null) },
                        modifier = Modifier.selectable(selected = activeCategory == category, role = Role.RadioButton) { selectedCategory = category.name; showCategories = false })
                }
            }
        }
    }
}

@Composable
private fun DiscoveryToolRow(tool: Tool, favorites: FavoritesStore, onOpenTool: (Tool) -> Unit, showCategory: Boolean) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth().clickable { onOpenTool(tool) }.padding(start = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(tool.icon, null, Modifier.padding(10.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f)) {
                Text(tool.name, style = MaterialTheme.typography.titleSmall)
                Text(if (showCategory) tool.category.label else tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { favorites.toggle(tool.route) }) {
                val favorite = favorites.isFavorite(tool.route)
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    if (favorite) "Remove ${tool.name} from favorites" else "Add ${tool.name} to favorites",
                    tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Quick-access row for the last few opened tools, shown beneath discovery controls when no filters are active. */
@Composable
private fun RecentToolsRow(tools: List<Tool>, onOpenTool: (Tool) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.layout_recent_tools),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
