package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.logic.generatePassword
import com.jaguar.gearbox.logic.passwordStrength

private const val UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
private const val LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz"
private const val NUMBER_CHARS = "0123456789"
private const val SYMBOL_CHARS = "!@#$%^&*()-_=+[]{}"

@Composable
fun PasswordGeneratorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var length by rememberSaveable { mutableFloatStateOf(16f) }
    var useUpper by rememberSaveable { mutableStateOf(true) }
    var useLower by rememberSaveable { mutableStateOf(true) }
    var useNumbers by rememberSaveable { mutableStateOf(true) }
    var useSymbols by rememberSaveable { mutableStateOf(false) }
    var password by rememberSaveable { mutableStateOf("") }
    var generatedCharsetSize by rememberSaveable { mutableIntStateOf(0) }

    val enabledClasses = buildList {
        if (useUpper) add(UPPERCASE_CHARS)
        if (useLower) add(LOWERCASE_CHARS)
        if (useNumbers) add(NUMBER_CHARS)
        if (useSymbols) add(SYMBOL_CHARS)
    }
    val charset = enabledClasses.joinToString("")

    ToolScaffold(
        title = "Password Generator",
        icon = Tools.byRoute(Tools.ROUTE_PASSWORD_GENERATOR)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Length", style = MaterialTheme.typography.bodyMedium)
            Text(length.toInt().toString(), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(value = length, onValueChange = { length = it }, valueRange = 4f..64f)

        ToggleRow("Uppercase (A-Z)", useUpper) { useUpper = it }
        ToggleRow("Lowercase (a-z)", useLower) { useLower = it }
        ToggleRow("Numbers (0-9)", useNumbers) { useNumbers = it }
        ToggleRow("Symbols (!@#...)", useSymbols) { useSymbols = it }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                password = generatePassword(length.toInt(), enabledClasses)
                generatedCharsetSize = charset.length
            },
            enabled = charset.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text("  Generate")
        }

        if (charset.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Select at least one character type.", color = MaterialTheme.colorScheme.error)
        }

        if (password.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = password,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            val strength = passwordStrength(password.length, generatedCharsetSize)
            Text(strength.first, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { strength.second },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { context.copyToClipboard("Password", password, sensitive = true) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text(" Copy")
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
