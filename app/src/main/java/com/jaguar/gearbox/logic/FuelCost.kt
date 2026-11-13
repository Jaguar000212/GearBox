package com.jaguar.gearbox.logic

/** Trip fuel cost from distance, mileage (distance-units per fuel-unit), and price per fuel-unit. */
data class FuelCostResult(val fuelNeeded: Double, val totalCost: Double)

fun calculateFuelCost(distance: Double, mileage: Double, pricePerUnit: Double): FuelCostResult? {
    if (!distance.isFinite() || !mileage.isFinite() || !pricePerUnit.isFinite() ||
        distance < 0 || mileage <= 0 || pricePerUnit < 0
    ) return null
    val fuelNeeded = distance / mileage
    val totalCost = fuelNeeded * pricePerUnit
    if (!fuelNeeded.isFinite() || !totalCost.isFinite() ||
        (distance > 0 && fuelNeeded == 0.0) ||
        (fuelNeeded > 0 && pricePerUnit > 0 && totalCost == 0.0)
    ) return null
    return FuelCostResult(fuelNeeded, totalCost)
}
