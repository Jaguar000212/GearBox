package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold
import java.util.Locale
import kotlin.math.pow

@Composable
fun LoanCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var principal by rememberSaveable { mutableStateOf("") }
    var rate by rememberSaveable { mutableStateOf("") }
    var time by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("") }

    val decimalKeyboard = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    ToolScaffold(
        title = "Loan Calculator",
        icon = Tools.byRoute(Tools.ROUTE_LOAN)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        OutlinedTextField(
            value = principal,
            onValueChange = { principal = it },
            label = { Text("Principal amount") },
            keyboardOptions = decimalKeyboard,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = rate,
            onValueChange = { rate = it },
            label = { Text("Rate of interest (% per year)") },
            keyboardOptions = decimalKeyboard,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text("Loan term (years)") },
            keyboardOptions = decimalKeyboard,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { result = computeLoanResult(principal, rate, time) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Calculate") }

        if (result.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { principal = ""; rate = ""; time = ""; result = "" },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Clear, contentDescription = null)
                Text(" Clear")
            }
            OutlinedButton(
                onClick = {
                    if (result.isNotEmpty()) context.copyToClipboard(
                        "Loan Calculator",
                        shareBody(principal, rate, time, result)
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text(" Copy")
            }
            OutlinedButton(
                onClick = {
                    if (result.isNotEmpty()) context.shareText(
                        shareBody(
                            principal,
                            rate,
                            time,
                            result
                        )
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Text(" Share")
            }
        }
    }
}

private fun shareBody(principal: String, rate: String, time: String, result: String): String =
    "Principal Amount: $principal\n" +
            "Rate of Interest: $rate%\n" +
            "Loan Term: ${time}Years\n" +
            result

/** Replicates the EMI/interest math from the Java `LoanCalculator`. */
private fun computeLoanResult(principalText: String, rateText: String, timeText: String): String {
    if (principalText.isEmpty() || rateText.isEmpty() || timeText.isEmpty()) {
        return "Required fields are empty"
    }
    return try {
        val loanAmount = principalText.toDouble()
        val interestRate = rateText.toDouble()
        val loanTerm = timeText.toDouble()

        val monthlyInterestRate = interestRate / 100 / 12
        val tenure = loanTerm * 12
        val emi = loanAmount * monthlyInterestRate * (1 + monthlyInterestRate).pow(tenure) /
                ((1 + monthlyInterestRate).pow(tenure) - 1)

        val totalAmount = emi * loanTerm * 12
        val totalInterest = totalAmount - loanAmount
        val interestPercentage = totalInterest / totalAmount * 100

        String.format(
            Locale.getDefault(),
            "Monthly Installment: %.2f\nTotal Amount Repayable: %.2f\nTotal Interest Repayable: %.2f\nInterest Percentage: %.2f%%",
            emi, totalAmount, totalInterest, interestPercentage,
        )
    } catch (_: NumberFormatException) {
        "Invalid input"
    }
}
