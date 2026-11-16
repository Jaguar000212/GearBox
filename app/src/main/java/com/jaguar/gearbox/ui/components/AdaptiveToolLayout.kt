package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * One reading order, two presentations. The tool scaffold owns scrolling: no nested scroll
 * regions or fixed-height panes. Large text deliberately returns to a single readable column.
 */
@Composable
fun AdaptiveToolLayout(
    modifier: Modifier = Modifier,
    input: @Composable ColumnScope.() -> Unit,
    result: @Composable ColumnScope.() -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (useSideBySideToolLayout(maxWidth.value, fontScale)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f).semantics { isTraversalGroup = true }, content = input)
                Column(Modifier.weight(1f).semantics { isTraversalGroup = true }, content = result)
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.fillMaxWidth(), content = input)
                Column(Modifier.fillMaxWidth(), content = result)
            }
        }
    }
}

/** Keep each pane comfortable rather than assuming landscape always means wide enough. */
internal fun useSideBySideToolLayout(widthDp: Float, fontScale: Float): Boolean =
    widthDp >= 720f && fontScale <= 1.3f
