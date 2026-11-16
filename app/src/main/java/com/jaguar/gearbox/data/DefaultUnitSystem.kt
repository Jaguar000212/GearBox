package com.jaguar.gearbox.data

/** Starting preference only; saved tool selections and in-progress forms take priority. */
enum class DefaultUnitSystem {
    METRIC, IMPERIAL;
    companion object {
        fun fromStored(value: String?): DefaultUnitSystem = entries.firstOrNull { it.name == value } ?: METRIC
    }
}
