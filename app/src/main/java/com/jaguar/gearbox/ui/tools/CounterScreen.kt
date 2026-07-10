package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun CounterScreen(onNavigateBack: () -> Unit) {
    var count by rememberSaveable { mutableIntStateOf(0) }

    ToolScaffold(
        title = "Counter",
        icon = Tools.byRoute(Tools.ROUTE_COUNTER)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalButton(onClick = { count-- }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrement")
            }
            FilledTonalButton(onClick = { count++ }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Add, contentDescription = "Increment")
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { count = 0 },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text("  Reset")
        }
    }
}
