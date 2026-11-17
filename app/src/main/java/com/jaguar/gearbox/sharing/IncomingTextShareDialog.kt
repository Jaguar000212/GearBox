package com.jaguar.gearbox.sharing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.IncomingTextDestination
import com.jaguar.gearbox.data.localizedLabel
import com.jaguar.gearbox.ui.components.CompactChoiceSelector

@Composable
fun IncomingTextShareDialog(
    share: IncomingTextShare,
    onDismiss: () -> Unit,
    onImport: (String, Map<String, String>) -> Unit,
) {
    val context = LocalContext.current
    var destination by remember(share.id) { mutableStateOf(IncomingTextDestination.TEXT) }
    var consumed by remember(share.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!consumed) { consumed = true; onDismiss() } },
        title = { Text(context.getString(R.string.incoming_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (share.text == null) Text(context.getString(R.string.incoming_rejected))
                else {
                    Text(context.getString(R.string.incoming_preview_note))
                    Text(share.text)
                    CompactChoiceSelector(
                        label = context.getString(R.string.incoming_destination), selected = destination,
                        choices = IncomingTextDestination.entries, choiceLabel = { it.localizedLabel(context) },
                        onSelect = { if (!consumed) destination = it },
                    )
                    Text(context.getString(R.string.incoming_replace_notice))
                    Text(context.getString(R.string.incoming_processing_notice))
                    Text(context.getString(R.string.incoming_retention_notice))
                }
            }
        },
        confirmButton = {
            if (share.text != null) TextButton(enabled = !consumed, onClick = {
                if (!consumed) destination.inputs(share.text)?.let { inputs ->
                    consumed = true
                    onImport(destination.route, inputs)
                }
            }) { Text(context.getString(R.string.incoming_import_to, destination.localizedLabel(context))) }
        },
        dismissButton = { TextButton(enabled = !consumed, onClick = {
            if (!consumed) { consumed = true; onDismiss() }
        }) { Text(context.getString(R.string.incoming_cancel)) } },
    )
}
