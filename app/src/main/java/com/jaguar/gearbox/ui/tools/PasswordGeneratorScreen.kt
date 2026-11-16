package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.TextButton
import kotlin.math.roundToInt
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
    var length by rememberSaveable { mutableIntStateOf(16) }
    var useUpper by rememberSaveable { mutableStateOf(true) }
    var useLower by rememberSaveable { mutableStateOf(true) }
    var useNumbers by rememberSaveable { mutableStateOf(true) }
    var useSymbols by rememberSaveable { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var generatedCharsetSize by remember { mutableIntStateOf(0) }

    var customize by rememberSaveable { mutableStateOf(false) }

    val enabledClasses = buildList {
        if (useUpper) add(UPPERCASE_CHARS)
        if (useLower) add(LOWERCASE_CHARS)
        if (useNumbers) add(NUMBER_CHARS)
        if (useSymbols) add(SYMBOL_CHARS)
    }
    val charset = enabledClasses.joinToString("")

    ToolScaffold(
        title = context.getString(R.string.special_password_generator),
        icon = Tools.byRoute(Tools.ROUTE_PASSWORD_GENERATOR)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        OutlinedButton(onClick = { customize = true }, modifier = Modifier.fillMaxWidth()) {
            Text(context.resources.getQuantityString(R.plurals.special_password_length, length, length))
        }
        Text(context.resources.getQuantityString(R.plurals.special_password_types, enabledClasses.size, enabledClasses.size), style = MaterialTheme.typography.bodySmall)
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
            Text(context.getString(R.string.special_generate))
        }

        if (charset.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(context.getString(R.string.special_select_at_least_one_character_type), color = MaterialTheme.colorScheme.error)
        }

        if (password.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = password,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            val strength = passwordStrength(password.length, generatedCharsetSize)
            Text(context.getString(R.string.special_password_strength, context.getString(when (strength.second) {
                0.25f -> R.string.special_strength_weak
                0.5f -> R.string.special_strength_medium
                0.75f -> R.string.special_strength_strong
                1f -> R.string.special_strength_very_strong
                else -> R.string.special_strength_unavailable
            })), style = MaterialTheme.typography.labelLarge)
            Text(context.getString(R.string.special_an_estimate_not_a_security_guarantee), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { strength.second },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { context.copyToClipboard(context.getString(R.string.special_password), password, sensitive = true) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text(context.getString(R.string.special_copy))
            }
            TextButton(onClick = { password = ""; generatedCharsetSize = 0 }, modifier = Modifier.fillMaxWidth()) { Text(context.getString(R.string.special_clear_password)) }
        }
    }
    if (customize) AlertDialog(onDismissRequest = { customize = false },
        title = { Text(context.getString(R.string.special_customize_password)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(context.getString(R.string.calc_length), style = MaterialTheme.typography.bodyMedium)
            Text(length.toInt().toString(), style = MaterialTheme.typography.bodyMedium)
        }
        Slider(value = length.toFloat(), onValueChange = { length = it.roundToInt() }, valueRange = 4f..64f, steps = 59)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { if (length > 4) length-- }, enabled = length > 4) { Text(context.getString(R.string.special_shorter)) }
            OutlinedButton(onClick = { if (length < 64) length++ }, enabled = length < 64) { Text(context.getString(R.string.special_longer)) }
        }

        ToggleRow(context.getString(R.string.special_uppercase_a_z), useUpper) { useUpper = it }
        ToggleRow(context.getString(R.string.special_lowercase_a_z), useLower) { useLower = it }
        ToggleRow(context.getString(R.string.special_numbers_0_9), useNumbers) { useNumbers = it }
        ToggleRow(context.getString(R.string.special_symbols), useSymbols) { useSymbols = it }

        } },
        confirmButton = { TextButton(onClick = { customize = false }) { Text(context.getString(R.string.screen_done)) } },
    )

}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
