package com.jaguar.gearbox.data

/** Stable persisted values; unsupported/older values safely fall back to Home. */
enum class LaunchScreen(val route: String) {
    HOME("home"), FAVORITES("favorites");

    companion object {
        fun fromStored(value: String?): LaunchScreen = entries.firstOrNull { it.name == value } ?: HOME
    }
}
