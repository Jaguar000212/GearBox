package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** A visible current choice with a bounded, accessible chooser, not an editable text field. */
@Composable
fun <T> CompactChoiceSelector(
    label: String,
    selected: T,
    choices: List<T>,
    choiceLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    val triggerFocus = remember { FocusRequester() }
    var returnFocus by remember { mutableStateOf(false) }
    fun dismiss() { expanded = false; returnFocus = true }
    LaunchedEffect(expanded, returnFocus) {
        if (!expanded && returnFocus) {
            // Wait for the dialog to leave composition before returning keyboard focus.
            withFrameNanos { }
            runCatching { triggerFocus.requestFocus() }
            returnFocus = false
        }
    }
    OutlinedButton(onClick = { expanded = true }, modifier = modifier.focusRequester(triggerFocus).heightIn(min = 48.dp)) {
        Text(context.getString(com.jaguar.gearbox.R.string.shared_label_value, label, choiceLabel(selected)), Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
    }
    if (expanded) {
        AlertDialog(
            onDismissRequest = ::dismiss,
            title = { Text(label) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                    choices.forEach { choice ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .selectable(selected = selected == choice, role = Role.RadioButton) {
                                    // Dismiss first; the caller owns all input/state changes.
                                    dismiss()
                                    onSelect(choice)
                                }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected == choice, onClick = null)
                            Text(choiceLabel(choice), Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = ::dismiss) { Text(context.getString(com.jaguar.gearbox.R.string.shared_close)) } },
        )
    }
}
