package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
        DecimalField(
            value = distance,
            onValueChange = { distance = it },
            label = "Trip distance (km/mi)",
            placeholder = "e.g. 300",
        )
        Spacer(Modifier.height(12.dp))
        DecimalField(
            value = mileage,
            onValueChange = { mileage = it },
            label = "Mileage (distance per unit fuel)",
            placeholder = "e.g. 15",
        )
        Spacer(Modifier.height(12.dp))
        DecimalField(
            value = price,
            onValueChange = { price = it },
            label = "Fuel price per unit",
            placeholder = "e.g. 100",
        )

        if (distanceValue != null && mileageValue != null && priceValue != null) {
            val result = calculateFuelCost(distanceValue, mileageValue, priceValue)
            if (result != null) {
                val summary = "Fuel needed: ${formatTrimmed(result.fuelNeeded, 2)}\n" +
                        "Total cost: ${formatTrimmed(result.totalCost, 2)}"

                Spacer(Modifier.height(20.dp))
                ResultCard(
                    text = summary,
                    onCopy = { context.copyToClipboard("Fuel cost", summary) },
                    onShare = { context.shareText(summary) },
                )
            }
        } else if (distance.isNotBlank() && mileage.isNotBlank() && price.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Enter a valid distance, mileage (greater than 0), and fuel price.",
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
