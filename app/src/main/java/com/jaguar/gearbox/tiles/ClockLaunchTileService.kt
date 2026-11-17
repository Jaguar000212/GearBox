package com.jaguar.gearbox.tiles

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.jaguar.gearbox.R

/** Manual-add, launch-only tiles. No timers, notifications, sensors or storage are touched here. */
abstract class ClockLaunchTileService : TileService() {
    protected abstract val target: ClockTileLaunch
    private var destroyed = false

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = getString(if (target == ClockTileLaunch.TIMER) R.string.quick_tile_timer else R.string.quick_tile_stopwatch)
            subtitle = getString(R.string.quick_tile_open_app)
            contentDescription = getString(R.string.quick_tile_launch_description, label)
            stateDescription = getString(R.string.quick_tile_launch_only)
            state = Tile.STATE_INACTIVE // Never misrepresents the running state of a clock.
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        // Opening GearBox can reveal private drafts; do not display them over the lock screen.
        if (isLocked) unlockAndRun { if (!destroyed) launchTool() }
        else if (!destroyed) launchTool()
    }

    private fun launchTool() {
        try {
            val intent = buildClockTileIntent(this, target)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pending = PendingIntent.getActivity(this, target.requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                startActivityAndCollapse(pending)
            } else {
                launchLegacy(intent)
            }
        } catch (_: RuntimeException) {
            Toast.makeText(this, R.string.quick_tile_launch_failed, Toast.LENGTH_SHORT).show()
        }
    }

    /** Intent launch is supported only before API 34; newer devices use PendingIntent above. */
    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    private fun launchLegacy(intent: Intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        destroyed = true
        super.onDestroy()
    }
}

class TimerLaunchTileService : ClockLaunchTileService() {
    override val target = ClockTileLaunch.TIMER
}
class StopwatchLaunchTileService : ClockLaunchTileService() {
    override val target = ClockTileLaunch.STOPWATCH
}
