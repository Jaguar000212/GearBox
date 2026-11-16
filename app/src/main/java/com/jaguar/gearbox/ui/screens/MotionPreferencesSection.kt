package com.jaguar.gearbox.ui.screens

import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.jaguar.gearbox.data.UiPreferencesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun MotionPreferencesSection() {
    val context = LocalContext.current
    val store = remember(context) { UiPreferencesStore(context) }
    val preferences by store.preferences.collectAsState()
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text("Reduce motion") },
        supportingContent = { Text(if (error) "Could not save. Please try again."
            else "Skip decorative coin and bottle spins. Results stay the same; system animation settings still apply.") },
        trailingContent = {
            Switch(checked = preferences.reduceMotion, enabled = !saving, onCheckedChange = { value ->
                if (!saving) {
                    saving = true
                    scope.launch {
                        try { error = !withContext(Dispatchers.IO) { store.setReduceMotion(value) } }
                        finally { saving = false }
                    }
                }
            })
        },
    )
}

@Composable
internal fun ActiveClockPreference() {
    val context = LocalContext.current
    val store = remember(context) { UiPreferencesStore(context) }
    val preferences by store.preferences.collectAsState()
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text("Show active clock on Home") },
        supportingContent = { Text(if (error) "Could not save. Please try again."
            else "Show running, paused or finished clock status. Hiding it does not stop the clock.") },
        trailingContent = {
            Switch(checked = preferences.showActiveClockShortcut, enabled = !saving, onCheckedChange = { value ->
                if (!saving) {
                    saving = true
                    scope.launch {
                        try { error = !withContext(Dispatchers.IO) { store.setShowActiveClockShortcut(value) } }
                        finally { saving = false }
                    }
                }
            })
        },
    )
}
