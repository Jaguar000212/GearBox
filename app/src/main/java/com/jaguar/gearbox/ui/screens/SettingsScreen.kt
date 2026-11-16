package com.jaguar.gearbox.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.data.RecentToolsStore
import com.jaguar.gearbox.data.LaunchScreen
import androidx.core.net.toUri
import com.jaguar.gearbox.R
import com.jaguar.gearbox.ui.theme.ThemeMode

private const val GITHUB_USERNAME = "Jaguar000212"
private const val GITHUB_PROFILE_URL = "https://github.com/$GITHUB_USERNAME"

/** Settings / about screen: theme, dynamic colour, haptics, and the app version. */
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    dynamicColorEnabled: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
    hapticsEnabled: Boolean,
    onHapticsChange: (Boolean) -> Unit,
    launchScreen: LaunchScreen = LaunchScreen.HOME,
    onLaunchScreenChange: (LaunchScreen) -> Unit = {},
    recentTools: RecentToolsStore? = null,
) {
    var showLaunchChoice by rememberSaveable { mutableStateOf(false) }
    var showThemeChoice by rememberSaveable { mutableStateOf(false) }
    var showClockOptions by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val recents = recentTools ?: remember(context) { RecentToolsStore(context) }
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "-"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(context.getString(R.string.screen_make_it_yours), style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
        Text(context.getString(R.string.screen_your_toolbox_your_preferences_history_presets_and_workbench_saves),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp))
        SectionHeader(stringResource(R.string.launch_section))
        ListItem(
            headlineContent = { Text(stringResource(R.string.launch_screen_title)) },
            supportingContent = { Text(stringResource(R.string.launch_screen_summary, launchLabel(launchScreen))) },
            modifier = Modifier.clickable { showLaunchChoice = true },
        )
        HorizontalDivider()
        HomePreferencesSection(recents)
        ActiveClockPreference()
        WidgetDiscoveryPreference()
        HorizontalDivider()
        SectionHeader(context.getString(R.string.screen_appearance))
        ListItem(
            headlineContent = { Text(context.getString(R.string.screen_theme)) },
            supportingContent = { Text(context.getString(themeMode.labelResource())) },
            leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
            modifier = Modifier.clickable { showThemeChoice = true },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ListItem(
                headlineContent = { Text(context.getString(R.string.screen_dynamic_colour)) },
                supportingContent = { Text(context.getString(R.string.screen_theme_follows_your_wallpaper_colours)) },
                leadingContent = { Icon(Icons.Filled.ColorLens, contentDescription = null) },
                trailingContent = {
                    Switch(checked = dynamicColorEnabled, onCheckedChange = onDynamicColorChange)
                },
            )
        }

        HorizontalDivider()
        SectionHeader(context.getString(R.string.screen_clocks))
        ListItem(
            headlineContent = { Text(context.getString(R.string.screen_clock_options)) },
            supportingContent = { Text(context.getString(R.string.screen_notifications_live_updates_and_completion_alerts)) },
            modifier = Modifier.clickable { showClockOptions = true },
        )
        HorizontalDivider()
        SectionHeader(context.getString(R.string.screen_feedback))
        MotionPreferencesSection()
        ListItem(
            headlineContent = { Text(context.getString(R.string.screen_haptic_feedback)) },
            supportingContent = { Text(context.getString(R.string.screen_vibrate_on_button_presses_and_long_actions)) },
            leadingContent = { Icon(Icons.Filled.Vibration, contentDescription = null) },
            trailingContent = {
                Switch(checked = hapticsEnabled, onCheckedChange = onHapticsChange)
            },
        )

        HorizontalDivider()
        DefaultUnitPreference()
        HorizontalDivider()
        ToolMemorySettingsSection()
        HorizontalDivider()
        SectionHeader(context.getString(R.string.screen_about))
        ListItem(
            headlineContent = { Text(context.getString(R.string.screen_brand_name)) },
            supportingContent = { Text(context.getString(R.string.screen_version_1_s, versionName)) },
            leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
        )
        GitHubCredit(username = GITHUB_USERNAME, profileUrl = GITHUB_PROFILE_URL)
    }
    if (showClockOptions) ClockOptionsDialog { showClockOptions = false }
    if (showThemeChoice) {
        AlertDialog(
            onDismissRequest = { showThemeChoice = false },
            title = { Text(context.getString(R.string.screen_theme)) },
            text = {
                Column(Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { mode ->
                        Row(Modifier.fillMaxWidth().selectable(
                            selected = themeMode == mode, role = Role.RadioButton,
                            onClick = { onThemeModeChange(mode); showThemeChoice = false },
                        ).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = themeMode == mode, onClick = null)
                            Text(context.getString(mode.labelResource()), Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeChoice = false }) { Text(context.getString(R.string.screen_close)) } },
        )
    }
    if (showLaunchChoice) {
        AlertDialog(
            onDismissRequest = { showLaunchChoice = false },
            title = { Text(stringResource(R.string.launch_screen_title)) },
            text = {
                Column(Modifier.selectableGroup()) {
                    Text(stringResource(R.string.launch_screen_help))
                    LaunchScreen.entries.forEach { screen ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .selectable(selected = screen == launchScreen, role = Role.RadioButton) {
                                    onLaunchScreenChange(screen)
                                    showLaunchChoice = false
                                }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = screen == launchScreen, onClick = null)
                            Text(launchLabel(screen), Modifier.padding(start = 12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLaunchChoice = false }) {
                    Text(stringResource(R.string.launch_screen_close))
                }
            },
        )
    }

}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun GitHubCredit(username: String, profileUrl: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, profileUrl.toUri()))
            }
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = context.getString(R.string.screen_gearbox_on_github),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
        }
        Text(
            text = context.getString(R.string.screen_made_with_by),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "@$username",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun launchLabel(screen: LaunchScreen): String = stringResource(
    if (screen == LaunchScreen.HOME) R.string.launch_screen_home else R.string.launch_screen_favorites,
)


/** Discovery only: the launcher owns adding and placing widgets. */
@Composable
internal fun WidgetDiscoveryPreference() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var open by rememberSaveable { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(context.getString(R.string.screen_home_screen_widgets)) },
        supportingContent = { Text(context.getString(R.string.screen_find_counter_and_timer_stopwatch_on_your_launcher)) },
        modifier = Modifier.clickable { open = true },
    )
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(context.getString(R.string.screen_home_screen_widgets)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(context.getString(R.string.screen_counter_view_and_change_the_same_count_used_in_the_app))
                Text(context.getString(R.string.screen_timer_stopwatch_view_the_selected_clock_and_use_its_controls_the))
                Text(context.getString(R.string.screen_to_add_one_long_press_an_empty_area_of_your_android_home_screen_c))
                Text(context.getString(R.string.screen_if_you_installed_gearbox_preview_alongside_gearbox_look_for_the_s))
                Text(context.getString(R.string.screen_this_guide_does_not_add_a_widget_or_change_any_running_clock_widg))
            }
        },
        confirmButton = { TextButton(onClick = { open = false }) { Text(context.getString(R.string.screen_got_it)) } },
    )
}


private fun ThemeMode.labelResource(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.screen_theme_system
    ThemeMode.LIGHT -> R.string.screen_theme_light
    ThemeMode.DARK -> R.string.screen_theme_dark
}
