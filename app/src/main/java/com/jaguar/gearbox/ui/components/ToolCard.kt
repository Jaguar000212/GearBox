package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tool

/**
 * A single tool card shown in the grid. Tapping opens the tool; the star toggles the favourite
 * state. The description is always visible rather than hidden behind a long-press, since the
 * card has room for it and users shouldn't have to discover a gesture to see what a tool does.
 */
@Composable
fun ToolCard(
    tool: Tool,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(188.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.name,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = tool.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tool.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
