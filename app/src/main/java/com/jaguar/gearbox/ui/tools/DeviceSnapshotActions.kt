package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.ui.components.ResultCard

/** An explicit capture, never a stream of sensor readings or a hardware-restoration command. */
@Composable
internal fun DeviceSnapshotActions(
    captureLabel: String,
    enabled: Boolean,
    capturedOutput: String,
    onCapture: () -> Unit,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onCapture, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(captureLabel)
        }
        Text(context.getString(com.jaguar.gearbox.R.string.shared_device_history_disclosure),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (capturedOutput.isNotEmpty()) {
            Text(context.getString(com.jaguar.gearbox.R.string.shared_captured_snapshot), style = MaterialTheme.typography.titleSmall)
            ResultCard(text = capturedOutput,
                onCopy = { context.copyToClipboard(context.getString(com.jaguar.gearbox.R.string.shared_device_snapshot), capturedOutput) },
                onShare = { context.shareText(capturedOutput) })
            TextButton(onClick = onClear) { Text(context.getString(com.jaguar.gearbox.R.string.shared_dismiss_snapshot)) }
        }
    }
}
