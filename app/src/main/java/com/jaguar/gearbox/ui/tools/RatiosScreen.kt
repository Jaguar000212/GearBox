package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.simplifyRatio
import com.jaguar.gearbox.ui.components.DecimalField
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun RatiosScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var partA by rememberSaveable { mutableStateOf("") }
    var partB by rememberSaveable { mutableStateOf("") }
    val a = partA.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val b = partB.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val result = if (a != null && b != null) simplifyRatio(a, b) else null

    ToolScaffold(title = "Ratios", icon = Tools.byRoute(Tools.ROUTE_RATIOS)!!.icon,
        onNavigateBack = onNavigateBack) {
        Text("Simplify a ratio", style = MaterialTheme.typography.titleLarge)
        Text("Keep the values in order. For example, 12 : 18 simplifies to 2 : 3.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        DecimalField(value = partA, onValueChange = { partA = it }, label = "First value", placeholder = "e.g. 12")
        Spacer(Modifier.height(12.dp))
        DecimalField(value = partB, onValueChange = { partB = it }, label = "Second value", placeholder = "e.g. 18")
        Spacer(Modifier.height(20.dp))
        if (result != null) {
            Text("Simplified ratio", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val summary = "${partA.trim()} : ${partB.trim()} = $result"
            ResultCard(text = result, textStyle = MaterialTheme.typography.headlineLarge,
                onCopy = { context.copyToClipboard("Ratio", summary) },
                onShare = { context.shareText(summary) })
            Spacer(Modifier.height(8.dp))
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        } else {
            val guidance = when {
                (partA.isNotBlank() && a == null) || (partB.isNotBlank() && b == null) ->
                    "Enter valid, finite numbers in both fields."
                partA.isBlank() || partB.isBlank() ->
                    "Enter both values to see the simplified ratio."
                else ->
                    "These values cannot be simplified. Check that the ratio is defined and within the supported range."
            }
            Text(guidance, style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = { partA = ""; partB = "" }, enabled = partA.isNotEmpty() || partB.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()) { Text("Clear") }
    }
}
