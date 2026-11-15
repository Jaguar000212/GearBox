package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.logic.TextCase
import com.jaguar.gearbox.logic.convertCase
import java.util.Locale
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.text.selection.SelectionContainer
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolActions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TextToolsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var textCase by rememberSaveable { mutableStateOf(TextCase.TITLE.name) }
    var caseExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedCase = TextCase.valueOf(textCase)

    val words = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val converted = remember(input, selectedCase) { convertCase(input, selectedCase) }
    val normalizedForPalindrome = input.filter { it.isLetterOrDigit() }.lowercase(Locale.ROOT)
    val isPalindrome =
        normalizedForPalindrome.isNotEmpty() && normalizedForPalindrome == normalizedForPalindrome.reversed()

    ToolScaffold(
        title = "Text Tools",
        icon = Tools.byRoute(Tools.ROUTE_TEXT_TOOLS)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Text") },
            placeholder = { Text("Type or paste text here") },
            minLines = 3,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth(),
        )

        if (input.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatColumn("Words", words.size.toString())
                    StatColumn("UTF-16 units", input.length.toString())
                    StatColumn("No spaces", input.count { !it.isWhitespace() }.toString())
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isPalindrome) "Palindrome ✓" else "Not a palindrome",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPalindrome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            ExposedDropdownMenuBox(
                expanded = caseExpanded,
                onExpandedChange = { caseExpanded = it },
            ) {
                OutlinedTextField(
                    value = selectedCase.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Convert to") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = caseExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                ExposedDropdownMenu(expanded = caseExpanded, onDismissRequest = { caseExpanded = false }) {
                    TextCase.entries.forEach { entry ->
                        DropdownMenuItem(
                            text = { Text(entry.label) },
                            onClick = {
                                textCase = entry.name
                                caseExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(selectedCase.label + " result", style = MaterialTheme.typography.titleMedium)
            SelectionContainer { ResultCard(text = converted) }
            Text("Palindrome ignores punctuation and case. Counts use UTF-16 units; camelCase uses ASCII letters and digits.",
                style = MaterialTheme.typography.bodySmall)

        }

        Spacer(Modifier.height(16.dp))
        ToolActions(
            onCopy = { context.copyToClipboard("Converted text", converted) },
            onShare = { context.shareText(converted) },
            onClear = { input = "" },
            resultEnabled = input.isNotEmpty(),
            clearEnabled = input.isNotEmpty(),
        )
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
