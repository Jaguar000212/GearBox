package com.jaguar.gearbox.data

import android.content.Context
import com.jaguar.gearbox.R

/** Localizes presentation without changing imported field names or values. */
fun ToolHandoff.localized(context: Context): ToolHandoff {
    val value = inputs["input"].orEmpty()
    val localizedDetail = when (route) {
        Tools.ROUTE_QR_GENERATOR -> context.getString(R.string.catalog_handoff_qr)
        Tools.ROUTE_HASH_GENERATOR -> context.getString(R.string.catalog_handoff_hash)
        Tools.ROUTE_TEXT_TOOLS -> context.getString(R.string.catalog_handoff_text)
        Tools.ROUTE_MORSE_BINARY -> context.getString(R.string.catalog_handoff_morse)
        Tools.ROUTE_BASE_CONVERTER -> context.getString(R.string.catalog_handoff_base, value)
        Tools.ROUTE_NUMBER_TO_ROMAN -> context.getString(R.string.catalog_handoff_roman, value)
        Tools.ROUTE_NUMBER_TO_WORDS, Tools.ROUTE_DECIMAL_TO_FRACTION -> context.getString(R.string.catalog_handoff_number, value)
        else -> detail
    }
    return copy(label = Tools.byRoute(route)?.localizedName(context) ?: label, detail = localizedDetail)
}
