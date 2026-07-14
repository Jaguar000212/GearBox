package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.simplifyRatio
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun RatiosScreen(onNavigateBack: () -> Unit) {
    var partA by rememberSaveable { mutableStateOf("") }
    var partB by rememberSaveable { mutableStateOf("") }

    val a = partA.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val b = partB.trim().toDoubleOrNull()?.takeIf { it.isFinite() }

    ToolScaffold(
        title = "Ratios",
        icon = Tools.byRoute(Tools.ROUTE_RATIOS)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = partA,
                onValueChange = { partA = it },
                label = { Text("First value") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = partB,
                onValueChange = { partB = it },
                label = { Text("Second value") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

        if (a != null && b != null && (a != 0.0 || b != 0.0)) {
            Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = simplifyRatio(a, b),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                )
            }
        } else if (partA.isNotBlank() && partB.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text("Enter valid numbers.", color = MaterialTheme.colorScheme.error)
        }
    }
}
