package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.computeLoanResult
import com.jaguar.gearbox.ui.components.DecimalField
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory

@Composable
fun LoanCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var principal by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("") }

    val loanAmount = principal.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    val interestRate = rate.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val loanTerm = time.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }

    val principalError =
        if (principal.isNotBlank() && loanAmount == null) context.getString(R.string.calc_enter_a_positive_amount) else null
    val rateError =
        if (rate.isNotBlank() && interestRate == null) context.getString(R.string.calc_enter_a_non_negative_rate) else null
    val timeError = if (time.isNotBlank() && loanTerm == null) context.getString(R.string.calc_enter_a_positive_term) else null

    // Live compute, like the rest of the app's calculators - a button here previously left a
    // stale result on screen after editing an input, still showing the answer to numbers you'd
    // already changed.
    val result = if (loanAmount != null && interestRate != null && loanTerm != null) {
        computeLoanResult(loanAmount, interestRate, loanTerm)
    } else {
        null
    }


    val history = rememberToolHistory(
        route = Tools.ROUTE_LOAN,
        inputs = mapOf("principal" to principal, "rate" to rate, "time" to time),
        output = result?.let { localizedLoanResult(context, it) },
        onRestore = { saved ->
            principal = saved["principal"] ?: ""
            rate = saved["rate"] ?: ""
            time = saved["time"] ?: ""
        },
    )

    ToolScaffold(
        title = context.getString(R.string.calc_loan_calculator),
        icon = Tools.byRoute(Tools.ROUTE_LOAN)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        AdaptiveToolLayout(input = {
            Text(stringResource(R.string.calc_monthly_repayment_estimate), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.calc_fixed_annual_rate_equal_monthly_payments_fees_are_not_i),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            DecimalField(
                value = principal,
                onValueChange = { principal = it },
                label = context.getString(R.string.calc_loan_amount),
                errorText = principalError,
            )
            Spacer(Modifier.height(8.dp))
            DecimalField(
                value = rate,
                onValueChange = { rate = it },
                label = context.getString(R.string.calc_annual_interest_rate),
                errorText = rateError,
            )
            Spacer(Modifier.height(8.dp))
            DecimalField(
                value = time,
                onValueChange = { time = it },
                label = context.getString(R.string.calc_loan_term_years),
                errorText = timeError,
            )
        }, result = {
            val displayResult = result?.let { localizedLoanResult(context, it, interestShareLabel = true) }
            if (displayResult != null) {
                Spacer(Modifier.height(16.dp))
                ResultCard(
                    text = displayResult,
                    onCopy = {
                        context.copyToClipboard(
                            context.getString(R.string.calc_loan_calculator),
                            shareBody(context, principal, rate, time, displayResult)
                        )
                    },
                    onShare = { context.shareText(shareBody(context, principal, rate, time, displayResult)) },
                )
            } else if (loanAmount != null && interestRate != null && loanTerm != null) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.calc_values_exceed_the_supported_numeric_range), color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            ToolActions(onClear = { principal = ""; rate = ""; time = "" },
                clearEnabled = principal.isNotEmpty() || rate.isNotEmpty() || time.isNotEmpty())
        })
    }
}

private fun shareBody(context: android.content.Context, principal: String, rate: String, time: String, result: String): String =
    context.getString(R.string.calc_principal_amount_1_s_n, principal) +
            context.getString(R.string.calc_rate_of_interest_1_s_n, rate) +
            context.getString(R.string.calc_loan_term_1_s_years_n, time) +
            result

/** Translate known original labels once; translated text is never fed through replacement again. */
internal fun localizedLoanResult(context: android.content.Context, result: String, interestShareLabel: Boolean = false): String {
    val labels = mapOf(
        "Monthly Installment:" to R.string.calc_monthly_installment_label,
        "Total Amount Repayable:" to R.string.calc_total_repayable_label,
        "Total Interest Repayable:" to R.string.calc_interest_repayable_label,
        "Interest Percentage:" to if (interestShareLabel) R.string.calc_interest_share_of_total_repayment else R.string.calc_interest_percentage_label,
    )
    return result.lines().joinToString("\n") { line ->
        val original = labels.keys.firstOrNull { line.startsWith(it) }
        if (original == null) line else context.getString(labels.getValue(original)) + line.removePrefix(original)
    }
}
