package com.jaguar.gearbox.logic

/** Trip fuel cost from distance, mileage (distance-units per fuel-unit), and price per fuel-unit. */
data class FuelCostResult(val fuelNeeded: Double, val totalCost: Double)

fun calculateFuelCost(distance: Double, mileage: Double, pricePerUnit: Double): FuelCostResult? {
    if (mileage <= 0.0) return null
    val fuelNeeded = distance / mileage
    return FuelCostResult(fuelNeeded, fuelNeeded * pricePerUnit)
}
