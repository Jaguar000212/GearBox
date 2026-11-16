package com.jaguar.gearbox.ui.screens

import com.jaguar.gearbox.R
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
        headlineContent = { Text(context.getString(R.string.screen_reduce_motion)) },
        supportingContent = { Text(if (error) context.getString(R.string.screen_could_not_save_please_try_again)
            else context.getString(R.string.screen_skip_decorative_coin_and_bottle_spins_results_stay_the_same_syste)) },
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
        headlineContent = { Text(context.getString(R.string.screen_show_active_clock_on_home)) },
        supportingContent = { Text(if (error) context.getString(R.string.screen_could_not_save_please_try_again)
            else context.getString(R.string.screen_show_running_paused_or_finished_clock_status_hiding_it_does_not_s)) },
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
