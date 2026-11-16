package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.data.ToolMemoryStore
import androidx.compose.runtime.remember
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.jaguar.gearbox.R
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
import com.jaguar.gearbox.ui.components.rememberToolHistory
import java.util.UUID

@Composable
fun ScreenRulerScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var useMetric by rememberSaveable { mutableStateOf(true) }

    val historyStore = remember(context) { ToolMemoryStore(context) }
    var captureGeneration by remember { mutableStateOf<Long?>(null) }
    var capturedOutput by remember { mutableStateOf("") }
    var capturedUnit by remember { mutableStateOf("") }
    var captureId by remember { mutableStateOf("") }
    val history = rememberToolHistory(
        route = Tools.ROUTE_SCREEN_RULER,
        inputs = mapOf("unit" to capturedUnit, "_capture" to captureId),
        output = capturedOutput.takeIf { it.isNotEmpty() },
        expectedHistoryGeneration = captureGeneration,
        onRestore = {}, allowRestore = false, restoreLastResult = false, recordingDelayMillis = 0L,
        modeInputs = mapOf("unit" to if (useMetric) "cm" else "in"),
        onRestoreMode = { values -> when (values["unit"]) { "cm" -> useMetric = true; "in" -> useMetric = false } },
    )

    // ydpi is the device's reported vertical pixel density - more accurate for a
    // physical-length ruler than deriving it from density-independent dp, which is quantized
    // into density buckets (mdpi/hdpi/xhdpi/...) and not a true physical measurement. This can
    // still be off on devices/OEMs that misreport DisplayMetrics.ydpi.
    val metrics = context.resources.displayMetrics
    val pxPerInch = metrics.ydpi.takeIf { it.isFinite() && it > 0f } ?: (metrics.densityDpi.toFloat())
    val pxPerCm = pxPerInch / 2.54f
    val pxPerUnit = if (useMetric) pxPerCm else pxPerInch
    val totalUnits = 30
    val labelPaddingPx = with(density) { 16.sp.toPx() }
    val totalHeightDp = with(density) { (pxPerUnit * totalUnits + labelPaddingPx * 2).toDp() }

    val outlineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurface
    val textSizePx = with(density) { 12.sp.toPx() }

    ToolScaffold(
        title = stringResource(R.string.ruler_title),
        icon = Tools.byRoute(Tools.ROUTE_SCREEN_RULER)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = useMetric, onClick = { useMetric = true }, label = { Text(stringResource(R.string.ruler_cm)) })
            FilterChip(selected = !useMetric, onClick = { useMetric = false }, label = { Text(stringResource(R.string.ruler_inches)) })
        }

        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.ruler_estimate_guidance),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(if (useMetric) R.string.ruler_metric_divisions else R.string.ruler_imperial_divisions),
            style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(16.dp))
        DeviceSnapshotActions(
            captureLabel = "Capture scale settings",
            enabled = true,
            capturedOutput = capturedOutput,
            onCapture = {
                capturedUnit = if (useMetric) "cm" else "in"
                captureGeneration = historyStore.historyGeneration(Tools.ROUTE_SCREEN_RULER)
                capturedOutput = "Estimated ruler scale: $capturedUnit, ${if (useMetric) "1 mm" else "1/16 inch"} subdivisions. Based on device-reported density. This captures the scale setup, not an object measurement. Verify against a physical ruler."
                captureId = UUID.randomUUID().toString()
            },
            onClear = { captureGeneration = null; capturedOutput = ""; capturedUnit = ""; captureId = "" },
        )
        Spacer(Modifier.height(16.dp))
        val minorSubdivisions = if (useMetric) 10 else 16
        val rulerDescription = stringResource(R.string.ruler_description, if (useMetric) "cm" else "in")
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeightDp)
                .semantics { contentDescription = rulerDescription },
        ) {
            val paint = android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = textSizePx
                isAntiAlias = true
            }
            for (unit in 0..totalUnits) {
                val y = labelPaddingPx + unit * pxPerUnit
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width * 0.6f, y),
                    strokeWidth = 3f,
                )
                drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(unit.toString(), size.width * 0.65f, y + textSizePx / 3, paint)
                    }
                if (unit < totalUnits) {
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
