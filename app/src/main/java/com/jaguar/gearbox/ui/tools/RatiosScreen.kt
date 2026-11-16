package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.ui.components.ShowWorking
import com.jaguar.gearbox.R
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
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory

@Composable
fun RatiosScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var partA by rememberSaveable { mutableStateOf("") }
    var partB by rememberSaveable { mutableStateOf("") }
    val a = partA.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val b = partB.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
    val result = if (a != null && b != null) simplifyRatio(a, b) else null

    val history = rememberToolHistory(
        route = Tools.ROUTE_RATIOS,
        inputs = mapOf("partA" to partA, "partB" to partB),
        output = result,
        onRestore = { saved ->
            partA = saved["partA"] ?: ""
            partB = saved["partB"] ?: ""
        },
    )

    ToolScaffold(title = context.getString(R.string.calc_ratios), icon = Tools.byRoute(Tools.ROUTE_RATIOS)!!.icon,
        onNavigateBack = onNavigateBack, history = history) {
        AdaptiveToolLayout(input = {
            Text(stringResource(R.string.calc_simplify_a_ratio), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.calc_keep_the_values_in_order_for_example_12_18_simplifies_t),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            DecimalField(value = partA, onValueChange = { partA = it }, label = context.getString(R.string.calc_first_value), placeholder = context.getString(R.string.calc_e_g_12))
            Spacer(Modifier.height(12.dp))
            DecimalField(value = partB, onValueChange = { partB = it }, label = context.getString(R.string.calc_second_value), placeholder = context.getString(R.string.calc_e_g_18))
            Spacer(Modifier.height(20.dp))
        }, result = {
            if (result != null) {
                Text(stringResource(R.string.calc_simplified_ratio), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val summary = context.getString(R.string.calc_1_s_2_s_3_s_2, partA.trim(), partB.trim(), result)
                ResultCard(text = result, textStyle = MaterialTheme.typography.headlineLarge,
                    onCopy = { context.copyToClipboard(context.getString(R.string.calc_ratio), summary) },
                    onShare = { context.shareText(summary) })
                Spacer(Modifier.height(8.dp))
                Text(summary, style = MaterialTheme.typography.bodyMedium)
                ShowWorking(ratioWorking(context, requireNotNull(a), requireNotNull(b)))
            } else {
                val guidance = when {
                    (partA.isNotBlank() && a == null) || (partB.isNotBlank() && b == null) ->
                        context.getString(R.string.calc_enter_valid_finite_numbers_in_both_fields)
                    partA.isBlank() || partB.isBlank() ->
                        context.getString(R.string.calc_enter_both_values_to_see_the_simplified_ratio)
                    else ->
                        context.getString(R.string.calc_these_values_cannot_be_simplified_check_that_the_ratio)
                }
                Text(guidance, style = MaterialTheme.typography.bodyMedium)
            }
            ToolActions(onClear = { partA = ""; partB = "" },
                clearEnabled = partA.isNotEmpty() || partB.isNotEmpty())
        })
    }
}
