package com.jaguar.gearbox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.R

/** Deterministic explanatory text only. Does not change tool output, exports or retention. */
@Composable
fun ShowWorking(steps: List<String>) {
    if (steps.isEmpty()) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    val state = stringResource(if (expanded) R.string.working_expanded else R.string.working_collapsed)
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.semantics { stateDescription = state }) {
                Text(stringResource(if (expanded) R.string.working_hide else R.string.working_show))
            }
            if (expanded) {
                Text(stringResource(R.string.working_precision), style = MaterialTheme.typography.bodySmall)
                steps.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
