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
) {
    var showLaunchChoice by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
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
        SectionHeader(stringResource(R.string.launch_section))
        ListItem(
            headlineContent = { Text(stringResource(R.string.launch_screen_title)) },
            supportingContent = { Text(stringResource(R.string.launch_screen_summary, launchLabel(launchScreen))) },
            modifier = Modifier.clickable { showLaunchChoice = true },
        )
        HorizontalDivider()
        SectionHeader("Appearance")
        ListItem(
            headlineContent = { Text("Theme") },
            supportingContent = { Text(themeMode.label) },
            leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                    label = { Text(mode.label) },
                )
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ListItem(
                headlineContent = { Text("Dynamic colour") },
                supportingContent = { Text("Theme follows your wallpaper colours.") },
                leadingContent = { Icon(Icons.Filled.ColorLens, contentDescription = null) },
                trailingContent = {
                    Switch(checked = dynamicColorEnabled, onCheckedChange = onDynamicColorChange)
                },
            )
        }

        HorizontalDivider()
        SectionHeader("Feedback")
        ListItem(
            headlineContent = { Text("Haptic feedback") },
            supportingContent = { Text("Vibrate on button presses and long actions.") },
            leadingContent = { Icon(Icons.Filled.Vibration, contentDescription = null) },
            trailingContent = {
                Switch(checked = hapticsEnabled, onCheckedChange = onHapticsChange)
            },
        )

        HorizontalDivider()
        SectionHeader("About")
        ListItem(
            headlineContent = { Text("GearBox") },
            supportingContent = { Text("Version $versionName") },
            leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
        )
        GitHubCredit(username = GITHUB_USERNAME, profileUrl = GITHUB_PROFILE_URL)
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
                contentDescription = "GearBox on GitHub",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
        }
        Text(
            text = "Made with ❤️ by",
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
