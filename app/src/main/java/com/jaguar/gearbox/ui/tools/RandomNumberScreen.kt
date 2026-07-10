package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import kotlin.random.Random

@Composable
fun RandomNumberScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var min by rememberSaveable { mutableStateOf("") }
    var max by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }

    ToolScaffold(
        title = "Random Number Generator",
        icon = Tools.byRoute(Tools.ROUTE_RANDOM)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        OutlinedTextField(
            value = min,
            onValueChange = { min = it },
            label = { Text("Minimum") },
            placeholder = { Text("e.g. 1") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = max,
            onValueChange = { max = it },
            label = { Text("Maximum") },
            placeholder = { Text("e.g. 100") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val low = min.trim().toLongOrNull()
                val high = max.trim().toLongOrNull()
                when {
                    low == null || high == null -> {
                        error = "Enter valid whole numbers for both bounds."
                        result = ""
                    }
                    low > high -> {
                        error = "Minimum must not be greater than maximum."
                        result = ""
                    }
                    else -> {
                        error = ""
                        // nextLong upper bound is exclusive, so add 1 to make it inclusive.
                        result = Random.nextLong(low, high + 1).toString()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Casino, contentDescription = null)
            Text("  Generate")
        }

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (result.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = result,
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { context.copyToClipboard("Random Number", result) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Text(" Copy")
                }
                OutlinedButton(
                    onClick = { context.shareText("Random number ($min–$max): $result") },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text(" Share")
                }
            }
        }
    }
}
