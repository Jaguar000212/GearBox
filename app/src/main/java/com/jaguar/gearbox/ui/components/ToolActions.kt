package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Consistent result actions. Supply only actions supported by a tool; clear stays secondary. */
@Composable
fun ToolActions(
    modifier: Modifier = Modifier,
    onCopy: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    resultEnabled: Boolean = true,
    clearEnabled: Boolean = true,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onCopy != null || onShare != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onCopy != null) {
                    OutlinedButton(onClick = onCopy, enabled = resultEnabled, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Copy")
                    }
                }
                if (onShare != null) {
                    OutlinedButton(onClick = onShare, enabled = resultEnabled, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }
                }
            }
        }
        if (onClear != null) {
            TextButton(onClick = onClear, enabled = clearEnabled, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Clear")
            }
        }
    }
}
