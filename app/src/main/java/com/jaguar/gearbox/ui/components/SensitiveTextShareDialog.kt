package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Review exactly what will leave the app; cancel/back never launches the chooser. */
@Composable
fun SensitiveTextShareDialog(
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    disclosure: String = "The external app you choose will receive this text. It may contain secrets or private information. Cancel if you need to remove anything first.",
    previewLabel: String = "Text to share",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review before sharing") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(disclosure)
                Text(previewLabel, style = MaterialTheme.typography.titleSmall)
                Text(text)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Continue to share") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
