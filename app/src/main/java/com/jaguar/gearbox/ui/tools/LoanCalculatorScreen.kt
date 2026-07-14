package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.jaguar.gearbox.ui.components.DecimalField
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolScaffold
import java.util.Locale
import kotlin.math.pow

@Composable
fun LoanCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var principal by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("") }

    val loanAmount = principal.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    val interestRate = rate.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val loanTerm = time.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }

    val principalError = if (principal.isNotBlank() && loanAmount == null) "Enter a positive amount." else null
    val rateError = if (rate.isNotBlank() && interestRate == null) "Enter a non-negative rate." else null
    val timeError = if (time.isNotBlank() && loanTerm == null) "Enter a positive term." else null

    // Live compute, like the rest of the app's calculators - a button here previously left a
    // stale result on screen after editing an input, still showing the answer to numbers you'd
    // already changed.
    val result = if (loanAmount != null && interestRate != null && loanTerm != null) {
        computeLoanResult(loanAmount, interestRate, loanTerm)
    } else {
        null
    }

    ToolScaffold(
        title = "Loan Calculator",
        icon = Tools.byRoute(Tools.ROUTE_LOAN)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        DecimalField(
            value = principal,
            onValueChange = { principal = it },
            label = "Principal amount",
            errorText = principalError,
        )
        Spacer(Modifier.height(8.dp))
        DecimalField(
            value = rate,
            onValueChange = { rate = it },
            label = "Rate of interest (% per year)",
            errorText = rateError,
        )
        Spacer(Modifier.height(8.dp))
        DecimalField(
            value = time,
            onValueChange = { time = it },
            label = "Loan term (years)",
            errorText = timeError,
        )

        if (result != null) {
            Spacer(Modifier.height(16.dp))
            ResultCard(
                text = result,
                onCopy = { context.copyToClipboard("Loan Calculator", shareBody(principal, rate, time, result)) },
                onShare = { context.shareText(shareBody(principal, rate, time, result)) },
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { principal = ""; rate = ""; time = "" },
                modifier = Modifier.weight(1f),
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

/** Replicates the EMI/interest math from the Java `LoanCalculator`. Inputs are pre-validated. */
private fun computeLoanResult(loanAmount: Double, interestRate: Double, loanTerm: Double): String {
    val monthlyInterestRate = interestRate / 100 / 12
    val tenure = loanTerm * 12
    // At 0% interest, the standard EMI formula divides by (1+r)^n - 1 = 0, producing NaN.
    // A zero-interest loan is just the principal split evenly across the term.
    val emi = if (monthlyInterestRate == 0.0) {
        loanAmount / tenure
    } else {
        loanAmount * monthlyInterestRate * (1 + monthlyInterestRate).pow(tenure) /
                ((1 + monthlyInterestRate).pow(tenure) - 1)
    }

    val totalAmount = emi * loanTerm * 12
    val totalInterest = totalAmount - loanAmount
    val interestPercentage = if (totalAmount == 0.0) 0.0 else totalInterest / totalAmount * 100

    return String.format(
        Locale.US,
        "Monthly Installment: %.2f\nTotal Amount Repayable: %.2f\nTotal Interest Repayable: %.2f\nInterest Percentage: %.2f%%",
        emi, totalAmount, totalInterest, interestPercentage,
    )
}
