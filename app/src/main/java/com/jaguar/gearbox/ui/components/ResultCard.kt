package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The app's standard "here's your result" card: a [Card] with the result text, and a Copy/Share
 * button row below it when the corresponding callback is supplied. Centralizing this fixes the
 * inconsistency where some tools had both actions, some had copy-only, and a few had neither -
 * and keeps error strings from ever needing to be rendered inside a card styled as a result.
 */
@Composable
fun ResultCard(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textAlign: TextAlign = TextAlign.Start,
    onCopy: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = text,
                style = textStyle,
                textAlign = textAlign,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }

        if (onCopy != null || onShare != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onCopy != null) {
                    OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        Text(" Copy")
                    }
                }
                if (onShare != null) {
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Text(" Share")
                    }
                }
            }
        }
    }
}
