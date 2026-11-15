package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.jaguar.gearbox.logic.calculateFuelCost
import com.jaguar.gearbox.logic.formatTrimmed
import com.jaguar.gearbox.ui.components.DecimalField
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun FuelCostScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var distance by rememberSaveable { mutableStateOf("") }
    var mileage by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }

    val distanceValue = distance.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val mileageValue = mileage.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    val priceValue = price.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }

    ToolScaffold(
        title = "Fuel Cost Calculator",
        icon = Tools.byRoute(Tools.ROUTE_FUEL_COST)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Text("Plan your trip cost", style = MaterialTheme.typography.titleMedium)
        Text("Keep units consistent: km with km/L and price/L, or miles with miles/gallon and price/gallon. No unit conversion is performed.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        DecimalField(
            value = distance,
            onValueChange = { distance = it },
            label = "Trip distance (km/mi)",
            placeholder = "e.g. 300",
            errorText = if (distance.isNotBlank() && distanceValue == null) "Enter a non-negative distance." else null,
        )
        Spacer(Modifier.height(12.dp))
        DecimalField(
            value = mileage,
            onValueChange = { mileage = it },
            label = "Mileage (distance per unit fuel)",
            placeholder = "e.g. 15",
            errorText = if (mileage.isNotBlank() && mileageValue == null) "Mileage must be greater than zero." else null,
        )
        Spacer(Modifier.height(12.dp))
        DecimalField(
            value = price,
            onValueChange = { price = it },
            label = "Fuel price per unit",
            placeholder = "e.g. 100",
            errorText = if (price.isNotBlank() && priceValue == null) "Enter a non-negative fuel price." else null,
        )

        if (distanceValue != null && mileageValue != null && priceValue != null) {
            val result = calculateFuelCost(distanceValue, mileageValue, priceValue)
            if (result != null) {
                val summary = "Fuel needed: ${formatTrimmed(result.fuelNeeded, 2)}\n" +
                        "Total cost: ${formatTrimmed(result.totalCost, 2)}"

                Spacer(Modifier.height(20.dp))
                Text("Estimated trip cost", style = MaterialTheme.typography.labelLarge)
                Text(formatTrimmed(result.totalCost, 2), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                ResultCard(
                    text = "Fuel needed: ${formatTrimmed(result.fuelNeeded, 2)} units\nCost uses the currency of your fuel price.",
                    onCopy = { context.copyToClipboard("Fuel cost", summary) },
                    onShare = { context.shareText(summary) },
                )
            } else {
                Spacer(Modifier.height(12.dp))
                Text("Values exceed the supported numeric range.", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(16.dp))
        androidx.compose.material3.TextButton(modifier = Modifier.fillMaxWidth(),
            onClick = { distance = ""; mileage = ""; price = "" },
            enabled = distance.isNotEmpty() || mileage.isNotEmpty() || price.isNotEmpty(),
        ) {
            Icon(Icons.Filled.Clear, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Clear")
        }
    }
}
