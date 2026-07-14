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
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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

private enum class NumberSystem(val label: String) {
    INTERNATIONAL("International"),
    INDIAN("Indian (Lakh/Crore)"),
}

@Composable
fun NumberToWordsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var numberSystem by rememberSaveable { mutableStateOf(NumberSystem.INTERNATIONAL.name) }
    val selectedSystem = NumberSystem.valueOf(numberSystem)

    val parsed = input.trim().toLongOrNull()
    val words = when {
        input.isBlank() -> null
        parsed == null -> null
        selectedSystem == NumberSystem.INDIAN -> numberToWordsIndian(parsed)
        else -> numberToWords(parsed)
    }
    val error = if (input.isNotBlank() && parsed == null) "Enter a valid whole number." else ""

    ToolScaffold(
        title = "Number to Words",
        icon = Tools.byRoute(Tools.ROUTE_NUMBER_TO_WORDS)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NumberSystem.entries.forEach { entry ->
                FilterChip(
                    selected = entry == selectedSystem,
                    onClick = { numberSystem = entry.name },
                    label = { Text(entry.label) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Number") },
            placeholder = { Text("e.g. 123456") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = error.isNotEmpty(),
            supportingText = { if (error.isNotEmpty()) Text(error) },
            modifier = Modifier.fillMaxWidth(),
        )

        if (words != null) {
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = words,
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
                onClick = { input = "" },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Clear, contentDescription = null)
                Text(" Clear")
            }
            OutlinedButton(
                onClick = { if (words != null) context.copyToClipboard("Number in words", words) },
                enabled = words != null,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Text(" Copy")
            }
        }
    }
}

private val ones = arrayOf(
    "",
    "One",
    "Two",
    "Three",
    "Four",
    "Five",
    "Six",
    "Seven",
    "Eight",
    "Nine",
    "Ten",
    "Eleven",
    "Twelve",
    "Thirteen",
    "Fourteen",
    "Fifteen",
    "Sixteen",
    "Seventeen",
    "Eighteen",
    "Nineteen",
)
private val tens = arrayOf(
    "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety",
)

/** Converts a number into English words using the short-scale (million, billion) grouping. */
private fun numberToWords(number: Long): String {
    if (number == 0L) return "Zero"

    val isNegative = number < 0
    // BigInteger.abs() avoids the Long.MIN_VALUE overflow that kotlin.math.abs() has (its magnitude
    // has no positive Long representation), and the grouping below never overflows either.
    var magnitude = java.math.BigInteger.valueOf(number).abs()
    val thousand = java.math.BigInteger.valueOf(1000)

    val groups = mutableListOf<Int>()
    while (magnitude > java.math.BigInteger.ZERO) {
        groups.add(magnitude.mod(thousand).toInt())
        magnitude /= thousand
    }

    // Long.MAX_VALUE has 19 digits, i.e. up to 7 groups of three (index 0-6), so this must cover
    // through "Quintillion" or grouping into it throws ArrayIndexOutOfBoundsException.
    val scales =
        arrayOf("", "Thousand", "Million", "Billion", "Trillion", "Quadrillion", "Quintillion")
    val parts = mutableListOf<String>()
    for (i in groups.indices.reversed()) {
        val group = groups[i]
        if (group == 0) continue
        val groupWords = threeDigitsToWords(group)
        parts.add(if (scales[i].isNotEmpty()) "$groupWords ${scales[i]}" else groupWords)
    }

    return (if (isNegative) "Negative " else "") + parts.joinToString(" ")
}

/**
 * Converts using the Indian numbering system (Thousand, Lakh, Crore, ...) instead of the
 * short-scale grouping in [numberToWords] - the app's Tambola audience reads 1,23,45,000 as
 * "One Crore Twenty-Three Lakh...", not "Twelve Million...".
 */
private fun numberToWordsIndian(number: Long): String {
    if (number == 0L) return "Zero"

    val isNegative = number < 0
    var magnitude = java.math.BigInteger.valueOf(number).abs()
    val thousand = java.math.BigInteger.valueOf(1000)
    val hundred = java.math.BigInteger.valueOf(100)

    // Only the last three digits ever get a "hundred" - every group above that is a plain
    // two-digit (00-99) group multiplied by its scale (thousand, lakh, crore, ...).
    val firstGroup = magnitude.mod(thousand).toInt()
    magnitude /= thousand

    val higherGroups = mutableListOf<Int>()
    while (magnitude > java.math.BigInteger.ZERO) {
        higherGroups.add(magnitude.mod(hundred).toInt())
        magnitude /= hundred
    }

    // Covers up to Shankh, which is more than enough for Long.MAX_VALUE (19 digits).
    val scales = arrayOf("", "Thousand", "Lakh", "Crore", "Arab", "Kharab", "Neel", "Padma", "Shankh")
    val parts = mutableListOf<String>()
    for (i in higherGroups.indices.reversed()) {
        val group = higherGroups[i]
        if (group == 0) continue
        parts.add("${twoDigitsToWords(group)} ${scales[i + 1]}")
    }
    if (firstGroup != 0) parts.add(threeDigitsToWords(firstGroup))

    return (if (isNegative) "Negative " else "") + parts.joinToString(" ")
}

private fun twoDigitsToWords(number: Int): String = when {
    number in 1..19 -> ones[number]
    number >= 20 -> {
        val tensPart = tens[number / 10]
        val onesPart = ones[number % 10]
        if (onesPart.isNotEmpty()) "$tensPart-$onesPart" else tensPart
    }
    else -> ""
}

private fun threeDigitsToWords(number: Int): String {
    val hundreds = number / 100
    val remainder = number % 100

    val parts = mutableListOf<String>()
    if (hundreds > 0) parts.add("${ones[hundreds]} Hundred")
    if (remainder in 1..19) {
        parts.add(ones[remainder])
    } else if (remainder >= 20) {
        val tensPart = tens[remainder / 10]
        val onesPart = ones[remainder % 10]
        parts.add(if (onesPart.isNotEmpty()) "$tensPart-$onesPart" else tensPart)
    }
    return parts.joinToString(" ")
}
