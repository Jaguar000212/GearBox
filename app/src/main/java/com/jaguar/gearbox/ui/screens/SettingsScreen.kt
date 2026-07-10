package com.jaguar.gearbox.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Settings / about screen (Compose port of the placeholder `Settings` fragment). */
@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        ListItem(
            headlineContent = { Text("Dynamic colour") },
            supportingContent = { Text("Theme follows your system wallpaper on Android 12+.") },
            leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
        )
        ListItem(
            headlineContent = { Text("About GearBox") },
            supportingContent = { Text("A suite of handy mini-tools. Kotlin + Jetpack Compose.") },
            leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
        )
    }
}
