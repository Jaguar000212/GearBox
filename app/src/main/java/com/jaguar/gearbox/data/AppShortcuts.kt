package com.jaguar.gearbox.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.jaguar.gearbox.MainActivity

/** Intent extra carrying the route to jump straight to when launched from a shortcut. */
const val EXTRA_SHORTCUT_ROUTE = "shortcut_route"

private const val MONOGRAM_BADGE_COLOR = "#6B5F10"

/**
 * Keeps the launcher's long-press shortcuts in sync with the user's favourited tools. A fixed
 * set of static shortcuts wouldn't reflect any individual user's actual favourites, so these are
 * rebuilt as dynamic shortcuts every time the favourites list changes.
 */
fun updateDynamicShortcuts(context: Context, favoriteRoutes: List<String>) {
    val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
        .takeIf { it > 0 } ?: 4

    val shortcuts = favoriteRoutes
        .mapNotNull(Tools::byRoute)
        .take(maxShortcuts)
        .mapIndexed { index, tool ->
            val intent = Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .putExtra(EXTRA_SHORTCUT_ROUTE, tool.route)

            ShortcutInfoCompat.Builder(context, tool.route)
                .setShortLabel(tool.name)
                .setLongLabel(tool.name)
                .setIcon(monogramIcon(tool.name))
                .setIntent(intent)
                .setRank(index)
                .build()
        }

    ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
}

/**
 * A simple initial-letter badge. Tool icons are Compose [androidx.compose.ui.graphics.vector.ImageVector]s
 * rendered inside a composition, not resources a launcher shortcut can point to directly, so this
 * draws a small monogram with plain [android.graphics] APIs instead.
 */
private fun monogramIcon(name: String): IconCompat {
    val size = 108
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(MONOGRAM_BADGE_COLOR)
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, backgroundPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = size * 0.5f
    }
    val letter = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(letter, size / 2f, textY, textPaint)

    return IconCompat.createWithBitmap(bitmap)
}
