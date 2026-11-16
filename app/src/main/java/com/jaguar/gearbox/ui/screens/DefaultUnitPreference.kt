package com.jaguar.gearbox.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.R
import com.jaguar.gearbox.data.DefaultUnitSystem
import com.jaguar.gearbox.data.UiPreferencesStore
import com.jaguar.gearbox.ui.components.CompactChoiceSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DefaultUnitPreference() {
    val context = LocalContext.current
    val store = remember(context) { UiPreferencesStore(context) }
    val preferences by store.preferences.collectAsState()
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp)) {
        CompactChoiceSelector(
            label = context.getString(R.string.unit_default_title),
            selected = preferences.defaultUnitSystem,
            choices = DefaultUnitSystem.entries,
            choiceLabel = { context.getString(if (it == DefaultUnitSystem.METRIC) R.string.unit_default_metric else R.string.unit_default_imperial) },
            onSelect = { value ->
                if (!saving) {
                    saving = true
                    scope.launch {
                        try { failed = !withContext(Dispatchers.IO) { store.setDefaultUnitSystem(value) } }
                        finally { saving = false }
                    }
                }
            },
        )
        Text(context.getString(R.string.unit_default_scope), style = MaterialTheme.typography.bodySmall)
        if (saving) Text(context.getString(R.string.unit_default_saving), style = MaterialTheme.typography.bodySmall)
        if (failed) Text(context.getString(R.string.unit_default_error), color = MaterialTheme.colorScheme.error)
    }
}
