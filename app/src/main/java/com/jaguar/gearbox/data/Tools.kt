package com.jaguar.gearbox.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PlusOne

/**
 * Central registry of the tools shown on the home screen. This is the Compose equivalent of the
 * Java `ToolList` class.
 */
object Tools {

    const val ROUTE_AVERAGE = "tool/average"
    const val ROUTE_LOAN = "tool/loan"
    const val ROUTE_COUNTER = "tool/counter"
    const val ROUTE_AGE = "tool/age"

    val all: List<Tool> = listOf(
        Tool(
            name = "Average Calculator",
            description = "Calculate average with some other interesting results.",
            icon = Icons.Filled.Calculate,
            route = ROUTE_AVERAGE,
        ),
        Tool(
            name = "Interest Calculator",
            description = "Calculates both simple and compound interests.",
            icon = Icons.Filled.Percent,
            route = ROUTE_LOAN,
        ),
        Tool(
            name = "Counter",
            description = "A simple counter to increment, decrement and reset.",
            icon = Icons.Filled.PlusOne,
            route = ROUTE_COUNTER,
        ),
        Tool(
            name = "Age Calculator",
            description = "Calculates the age between two dates.",
            icon = Icons.Filled.Cake,
            route = ROUTE_AGE,
        ),
    )

    fun byRoute(route: String?): Tool? = all.firstOrNull { it.route == route }
}
