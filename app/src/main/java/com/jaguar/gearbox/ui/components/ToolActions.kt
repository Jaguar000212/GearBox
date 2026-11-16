package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Consistent result actions. Supply only actions supported by a tool; clear stays secondary. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolActions(
    modifier: Modifier = Modifier,
    onCopy: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    resultEnabled: Boolean = true,
    clearEnabled: Boolean = true,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onCopy != null || onShare != null) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = if (LocalDensity.current.fontScale >= 1.5f) 1 else 2) {
                if (onCopy != null) {
                    OutlinedButton(onClick = onCopy, enabled = resultEnabled, modifier = Modifier.weight(1f).widthIn(min = 128.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(context.getString(com.jaguar.gearbox.R.string.shared_copy))
                    }
                }
                if (onShare != null) {
                    OutlinedButton(onClick = onShare, enabled = resultEnabled, modifier = Modifier.weight(1f).widthIn(min = 128.dp)) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(context.getString(com.jaguar.gearbox.R.string.shared_share))
                    }
                }
            }
        }
        if (onClear != null) {
            TextButton(onClick = onClear, enabled = clearEnabled, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(context.getString(com.jaguar.gearbox.R.string.shared_clear))
            }
        }
    }
}
