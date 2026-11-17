package com.jaguar.gearbox.tiles

import android.content.Context
import android.content.Intent
import com.jaguar.gearbox.MainActivity
import com.jaguar.gearbox.data.EXTRA_SHORTCUT_ROUTE

/** Presentation-only targets, deliberately distinct from notification/reset/action routes. */
enum class ClockTileLaunch(val route: String, val mode: String, val requestCode: Int) {
    TIMER("quick_tile_timer", "TIMER", 5101),
    STOPWATCH("quick_tile_stopwatch", "STOPWATCH", 5102);

    companion object {
        fun fromRoute(route: String?): ClockTileLaunch? = entries.firstOrNull { it.route == route }
    }
}

/** Explicit component uses the current install package, including isolated preview variants. */
fun buildClockTileIntent(context: Context, target: ClockTileLaunch): Intent =
    Intent(context, MainActivity::class.java)
        .setAction(Intent.ACTION_VIEW)
        .putExtra(EXTRA_SHORTCUT_ROUTE, target.route)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
