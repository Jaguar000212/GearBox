package com.jaguar.gearbox.ui.tools

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
        if (principal.isNotBlank() && loanAmount == null) "Enter a positive amount." else null
    val rateError =
        if (rate.isNotBlank() && interestRate == null) "Enter a non-negative rate." else null
    val timeError = if (time.isNotBlank() && loanTerm == null) "Enter a positive term." else null

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
        output = result,
        onRestore = { saved ->
            principal = saved["principal"] ?: ""
            rate = saved["rate"] ?: ""
            time = saved["time"] ?: ""
        },
    )

    ToolScaffold(
        title = "Loan Calculator",
        icon = Tools.byRoute(Tools.ROUTE_LOAN)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        Text("Monthly repayment estimate", style = MaterialTheme.typography.titleMedium)
        Text("Fixed annual rate, equal monthly payments. Fees are not included. Use one currency for all amounts.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        DecimalField(
            value = principal,
            onValueChange = { principal = it },
            label = "Loan amount",
            errorText = principalError,
        )
        Spacer(Modifier.height(8.dp))
        DecimalField(
            value = rate,
            onValueChange = { rate = it },
            label = "Annual interest rate (%)",
            errorText = rateError,
        )
        Spacer(Modifier.height(8.dp))
        DecimalField(
            value = time,
            onValueChange = { time = it },
            label = "Loan term (years)",
            errorText = timeError,
        )

        val displayResult = result?.replace("Interest Percentage:", "Interest share of total repayment:")
        if (displayResult != null) {
            Spacer(Modifier.height(16.dp))
            ResultCard(
                text = displayResult,
                onCopy = {
                    context.copyToClipboard(
                        "Loan Calculator",
                        shareBody(principal, rate, time, displayResult)
                    )
                },
                onShare = { context.shareText(shareBody(principal, rate, time, displayResult)) },
            )
        } else if (loanAmount != null && interestRate != null && loanTerm != null) {
            Spacer(Modifier.height(12.dp))
            Text("Values exceed the supported numeric range.", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.TextButton(
                onClick = { principal = ""; rate = ""; time = "" },
                modifier = Modifier.weight(1f),
                enabled = principal.isNotEmpty() || rate.isNotEmpty() || time.isNotEmpty(),
            ) {
                Icon(Icons.Filled.Clear, contentDescription = null)
                Text(" Clear")
            }
        }
    }
}

private fun shareBody(principal: String, rate: String, time: String, result: String): String =
    "Principal Amount: $principal\n" +
            "Rate of Interest: $rate%\n" +
            "Loan Term: $time Years\n" +
            result
