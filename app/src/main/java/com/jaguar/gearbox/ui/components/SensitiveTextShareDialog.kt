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
    disclosure: String? = null,
    previewLabel: String? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(com.jaguar.gearbox.R.string.shared_review_before_sharing)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(disclosure ?: context.getString(com.jaguar.gearbox.R.string.shared_the_external_app_you_choose_will_receive_this_text_it_may_contain_secr))
                Text(previewLabel ?: context.getString(com.jaguar.gearbox.R.string.shared_text_to_share), style = MaterialTheme.typography.titleSmall)
                Text(text)
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(context.getString(com.jaguar.gearbox.R.string.shared_continue_to_share)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(context.getString(com.jaguar.gearbox.R.string.shared_cancel)) } },
    )
}
