package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import kotlin.random.Random

@Composable
fun OneOfTwoScreen(onNavigateBack: () -> Unit) {
    var optionA by rememberSaveable { mutableStateOf("") }
    var optionB by rememberSaveable { mutableStateOf("") }
    // Keep the selected slot, not just its label: identical labels are still two choices.
    var chosenIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val summary = chosenIndex?.let { index ->
        "Option ${index + 1} selected\n${if (index == 0) optionA.trim() else optionB.trim()}"
    }

    ToolScaffold(
        title = "1 of 2",
        icon = Tools.byRoute(Tools.ROUTE_ONE_OF_TWO)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Text("Two choices. One decision.", style = MaterialTheme.typography.titleLarge)
        Text("Each option has an equal chance. Edit either option to start a new choice.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = optionA,
            onValueChange = { optionA = it; chosenIndex = null },
            label = { Text("Option 1") },
            minLines = 1, maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = optionB,
            onValueChange = { optionB = it; chosenIndex = null },
            label = { Text("Option 2") },
            minLines = 1, maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        if (optionA.isNotBlank() && optionA.trim() == optionB.trim()) {
            Spacer(Modifier.height(8.dp))
            Text("Both labels are the same. The selected option number will distinguish them.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { focusManager.clearFocus(); chosenIndex = Random.nextInt(2) },
            enabled = optionA.isNotBlank() && optionB.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Shuffle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (chosenIndex == null) "Pick one" else "Pick again")
        }
        Spacer(Modifier.height(20.dp))
        if (summary != null) {
            ResultCard(text = summary, textStyle = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        } else {
            Text("Enter both options, then pick one.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        ToolActions(
            onCopy = { summary?.let { context.copyToClipboard("1 of 2", it) } },
            onShare = { summary?.let { context.shareText(it) } },
            resultEnabled = summary != null,
            onClear = { optionA = ""; optionB = ""; chosenIndex = null },
            clearEnabled = optionA.isNotEmpty() || optionB.isNotEmpty(),
        )
    }
}
