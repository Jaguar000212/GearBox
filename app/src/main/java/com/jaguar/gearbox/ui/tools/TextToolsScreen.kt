package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.jaguar.gearbox.R
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.CompactChoiceSelector
import com.jaguar.gearbox.logic.TextCase
import com.jaguar.gearbox.logic.convertCase
import java.util.Locale
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.SensitiveTextShareDialog
import com.jaguar.gearbox.ui.components.ToolActions

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TextToolsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    fun caseLabel(value: TextCase): String = context.getString(when (value) {
        TextCase.UPPER -> R.string.special_case_upper
        TextCase.LOWER -> R.string.special_case_lower
        TextCase.TITLE -> R.string.special_case_title
        TextCase.CAMEL -> R.string.special_case_camel
    })
    var input by rememberSaveable { mutableStateOf("") }
    var textCase by rememberSaveable { mutableStateOf(TextCase.TITLE.name) }
    val selectedCase = TextCase.valueOf(textCase)
    var shareReview by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val words = input.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val converted = remember(input, selectedCase) { convertCase(input, selectedCase) }
    val normalizedForPalindrome = input.filter { it.isLetterOrDigit() }.lowercase(Locale.ROOT)
    val isPalindrome =
        normalizedForPalindrome.isNotEmpty() && normalizedForPalindrome == normalizedForPalindrome.reversed()

    val history = com.jaguar.gearbox.ui.components.rememberToolHistory(
        Tools.ROUTE_TEXT_TOOLS, mapOf("text" to input, "case" to textCase), converted.takeIf { input.isNotBlank() },
        onRestore = { values ->
            input = values["text"].orEmpty()
            TextCase.entries.firstOrNull { it.name == values["case"] }?.let { textCase = it.name }
        },
        modeInputs = mapOf("case" to textCase),
        onRestoreMode = { values -> TextCase.entries.firstOrNull { it.name == values["case"] }?.let { textCase = it.name } },
    )
    ToolScaffold(
        title = context.getString(R.string.special_text_tools),
        icon = Tools.byRoute(Tools.ROUTE_TEXT_TOOLS)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        AdaptiveToolLayout(input = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(context.getString(R.string.hash_input)) },
                placeholder = { Text(context.getString(R.string.special_type_or_paste_text_here)) },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Text(context.getString(R.string.text_group_transform),
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(context.getString(R.string.text_group_transform_help),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            CompactChoiceSelector(
                label = context.getString(R.string.calc_convert_to), selected = selectedCase, choices = TextCase.entries,
                choiceLabel = { caseLabel(it) }, onSelect = { textCase = it.name },
            )
            Text(context.getString(R.string.text_group_camel_help),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        }, result = {
            ToolActions(
                onCopy = { context.copyToClipboard(context.getString(R.string.special_converted_text), converted, sensitive = true) },
                onShare = { shareReview = Triple(input, textCase, converted) },
                onClear = { input = "" },
                resultEnabled = input.isNotEmpty(),
                clearEnabled = input.isNotEmpty(),
            )
            if (input.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(context.getString(R.string.special_text_result, caseLabel(selectedCase)), style = MaterialTheme.typography.titleMedium)
                ResultCard(text = converted, selectable = false)

                Spacer(Modifier.height(20.dp))
                Text(context.getString(R.string.text_group_information),
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Text(context.getString(R.string.text_group_information_help),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatColumn(context.getString(R.string.special_words), words.size.toString())
                            StatColumn(context.getString(R.string.special_utf_16_units), input.length.toString())
                            StatColumn(context.getString(R.string.special_no_spaces), input.count { !it.isWhitespace() }.toString())
                        }
                        Text(if (isPalindrome) context.getString(R.string.special_palindrome) else context.getString(R.string.special_not_a_palindrome),
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(context.getString(R.string.text_group_information_limits),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            } else Text(context.getString(R.string.special_enter_text_to_see_its_converted_form_and_copy_or_share_the_result),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        })
    }
    shareReview?.let { snapshot ->
        if (snapshot.first != input || snapshot.second != textCase || snapshot.third != converted) {
            LaunchedEffect(snapshot) { shareReview = null }
        } else SensitiveTextShareDialog(snapshot.third,
            onConfirm = {
                shareReview = null
                if (snapshot.first == input && snapshot.second == textCase && snapshot.third == converted)
                    context.shareText(snapshot.third)
            }, onDismiss = { shareReview = null })
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
