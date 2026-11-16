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
    var expanded by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = true }, modifier = modifier.heightIn(min = 48.dp)) {
        Text("$label: ${choiceLabel(selected)}", Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
    }
    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(label) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                    choices.forEach { choice ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .selectable(selected = selected == choice, role = Role.RadioButton) {
                                    // Dismiss first; the caller owns all input/state changes.
                                    expanded = false
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
            confirmButton = { TextButton(onClick = { expanded = false }) { Text("Close") } },
        )
    }
}
