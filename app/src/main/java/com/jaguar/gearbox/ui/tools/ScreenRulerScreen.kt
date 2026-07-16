package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.ui.components.ToolScaffold

@Composable
fun ScreenRulerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var useMetric by rememberSaveable { mutableStateOf(true) }

    // xdpi is the device's actual reported horizontal pixel density - more accurate for a
    // physical-length ruler than deriving it from density-independent dp, which is quantized
    // into density buckets (mdpi/hdpi/xhdpi/...) and not a true physical measurement. This can
    // still be off on devices/OEMs that misreport DisplayMetrics.xdpi.
    val metrics = context.resources.displayMetrics
    val pxPerInch = metrics.xdpi.takeIf { it > 0f } ?: (metrics.densityDpi.toFloat())
    val pxPerCm = pxPerInch / 2.54f
    val pxPerUnit = if (useMetric) pxPerCm else pxPerInch
    val totalUnits = 30
    val totalHeightDp = with(density) { (pxPerUnit * totalUnits).toDp() }

    val outlineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurface
    val textSizePx = with(density) { 12.sp.toPx() }

    ToolScaffold(
        title = "Screen Ruler",
        icon = Tools.byRoute(Tools.ROUTE_SCREEN_RULER)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = useMetric, onClick = { useMetric = true }, label = { Text("cm") })
            FilterChip(selected = !useMetric, onClick = { useMetric = false }, label = { Text("in") })
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Line up an object's edge with the top of the ruler below. Physical accuracy depends " +
                "on your device correctly reporting its screen density.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        val minorSubdivisions = if (useMetric) 10 else 16
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeightDp),
        ) {
            val paint = android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = textSizePx
                isAntiAlias = true
            }
            for (unit in 0..totalUnits) {
                val y = unit * pxPerUnit
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width * 0.6f, y),
                    strokeWidth = 3f,
                )
                if (unit < totalUnits) {
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(unit.toString(), size.width * 0.65f, y + textSizePx / 3, paint)
                    }
                    for (sub in 1 until minorSubdivisions) {
                        val subY = y + sub * (pxPerUnit / minorSubdivisions)
                        val isMid = sub == minorSubdivisions / 2
                        drawLine(
                            color = outlineColor,
                            start = Offset(0f, subY),
                            end = Offset(size.width * (if (isMid) 0.4f else 0.25f), subY),
                            strokeWidth = 1.5f,
                        )
                    }
                }
            }
        }
    }
}
