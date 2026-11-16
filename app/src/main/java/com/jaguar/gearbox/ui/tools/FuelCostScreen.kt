package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
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
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory

@Composable
fun FuelCostScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var distance by rememberSaveable { mutableStateOf("") }
    var mileage by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }

    val distanceValue = distance.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
    val mileageValue = mileage.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
    val priceValue = price.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }

    val historyResult = if (distanceValue != null && mileageValue != null && priceValue != null)
        calculateFuelCost(distanceValue, mileageValue, priceValue) else null

    val history = rememberToolHistory(
        route = Tools.ROUTE_FUEL_COST,
        inputs = mapOf("distance" to distance, "mileage" to mileage, "price" to price),
        output = historyResult?.let { context.getString(R.string.calc_fuel_needed_1_s_ntotal_cost_2_s, formatTrimmed(it.fuelNeeded, 2), formatTrimmed(it.totalCost, 2)) },
        onRestore = { saved ->
            distance = saved["distance"] ?: ""
            mileage = saved["mileage"] ?: ""
            price = saved["price"] ?: ""
        },
    )

    ToolScaffold(
        title = context.getString(R.string.calc_fuel_cost_calculator),
        icon = Tools.byRoute(Tools.ROUTE_FUEL_COST)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        AdaptiveToolLayout(input = {
            Text(stringResource(R.string.calc_plan_your_trip_cost), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.calc_keep_units_consistent_km_with_km_l_and_price_l_or_miles),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            DecimalField(
                value = distance,
                onValueChange = { distance = it },
                label = context.getString(R.string.calc_trip_distance_km_mi),
                placeholder = context.getString(R.string.calc_e_g_300),
                errorText = if (distance.isNotBlank() && distanceValue == null) context.getString(R.string.calc_enter_a_non_negative_distance) else null,
            )
            Spacer(Modifier.height(12.dp))
            DecimalField(
                value = mileage,
                onValueChange = { mileage = it },
                label = context.getString(R.string.calc_mileage_distance_per_unit_fuel),
                placeholder = context.getString(R.string.calc_e_g_15),
                errorText = if (mileage.isNotBlank() && mileageValue == null) context.getString(R.string.calc_mileage_must_be_greater_than_zero) else null,
            )
            Spacer(Modifier.height(12.dp))
            DecimalField(
                value = price,
                onValueChange = { price = it },
                label = context.getString(R.string.calc_fuel_price_per_unit),
                placeholder = context.getString(R.string.calc_e_g_100),
                errorText = if (price.isNotBlank() && priceValue == null) context.getString(R.string.calc_enter_a_non_negative_fuel_price) else null,
            )
        }, result = {
            if (distanceValue != null && mileageValue != null && priceValue != null) {
                val result = calculateFuelCost(distanceValue, mileageValue, priceValue)
                if (result != null) {
                    val summary = context.getString(R.string.calc_fuel_needed_1_s_n, formatTrimmed(result.fuelNeeded, 2)) +
                            context.getString(R.string.calc_total_cost_1_s, formatTrimmed(result.totalCost, 2))

                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.calc_estimated_trip_cost), style = MaterialTheme.typography.labelLarge)
                    Text(formatTrimmed(result.totalCost, 2), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    ResultCard(
                        text = context.getString(R.string.calc_fuel_needed_1_s_units_ncost_uses_the_currency_of_your_f, formatTrimmed(result.fuelNeeded, 2)),
                        onCopy = { context.copyToClipboard(context.getString(R.string.calc_fuel_cost), summary) },
                        onShare = { context.shareText(summary) },
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.calc_values_exceed_the_supported_numeric_range), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(16.dp))
            ToolActions(onClear = { distance = ""; mileage = ""; price = "" },
                clearEnabled = distance.isNotEmpty() || mileage.isNotEmpty() || price.isNotEmpty())
        })
    }
}
