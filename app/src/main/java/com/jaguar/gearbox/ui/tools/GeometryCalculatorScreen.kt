package com.jaguar.gearbox.ui.tools

import androidx.compose.ui.res.stringResource
import com.jaguar.gearbox.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.jaguar.gearbox.ui.components.ResultCard
import com.jaguar.gearbox.ui.components.DecimalField
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.formatTrimmed
import com.jaguar.gearbox.ui.components.AdaptiveToolLayout
import com.jaguar.gearbox.ui.components.ToolActions
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.CompactChoiceSelector
import com.jaguar.gearbox.ui.components.rememberToolHistory
import kotlin.math.PI
import kotlin.math.sqrt

private enum class Shape(val label: Int) {
    CIRCLE(R.string.calc_circle),
    RECTANGLE(R.string.calc_rectangle),
    TRIANGLE(R.string.calc_triangle),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeometryCalculatorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var shape by rememberSaveable { mutableStateOf(Shape.CIRCLE.name) }
    val selectedShape = Shape.valueOf(shape)

    var field1 by rememberSaveable { mutableStateOf("") }
    var field2 by rememberSaveable { mutableStateOf("") }
    var field3 by rememberSaveable { mutableStateOf("") }


    val history = rememberToolHistory(
        route = Tools.ROUTE_GEOMETRY,
        inputs = mapOf("shape" to shape, "field1" to field1, "field2" to field2, "field3" to field3),
        output = computeResult(context, selectedShape, field1, field2, field3),
        modeInputs = mapOf("shape" to shape),
        onRestoreMode = { saved ->
            shape = Shape.entries.firstOrNull { it.name == saved["shape"] }?.name ?: Shape.CIRCLE.name
        },
        onRestore = { saved ->
            shape = Shape.entries.firstOrNull { it.name == saved["shape"] }?.name ?: Shape.CIRCLE.name
            field1 = saved["field1"] ?: ""
            field2 = saved["field2"] ?: ""
            field3 = saved["field3"] ?: ""
        },
    )

    ToolScaffold(
        title = context.getString(R.string.calc_geometry_calculator),
        icon = Tools.byRoute(Tools.ROUTE_GEOMETRY)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        AdaptiveToolLayout(input = {
            CompactChoiceSelector(
                label = context.getString(R.string.calc_shape), selected = selectedShape, choices = Shape.entries,
                choiceLabel = { context.getString(it.label) }, onSelect = { if (shape != it.name) { field1 = ""; field2 = ""; field3 = "" }; shape = it.name },
            )

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.calc_use_the_same_unit_for_every_measurement_area_is_in_squa), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))

            when (selectedShape) {
                Shape.CIRCLE -> {
                    NumberField(context.getString(R.string.calc_radius), field1) { field1 = it }
                }

                Shape.RECTANGLE -> {
                    NumberField(context.getString(R.string.calc_width), field1) { field1 = it }
                    Spacer(Modifier.height(12.dp))
                    NumberField(context.getString(R.string.calc_height), field2) { field2 = it }
                }

                Shape.TRIANGLE -> {
                    NumberField(context.getString(R.string.calc_side_a), field1) { field1 = it }
                    Spacer(Modifier.height(12.dp))
                    NumberField(context.getString(R.string.calc_side_b), field2) { field2 = it }
                    Spacer(Modifier.height(12.dp))
                    NumberField(context.getString(R.string.calc_side_c), field3) { field3 = it }
                }
            }
        }, result = {
            val result = computeResult(context, selectedShape, field1, field2, field3)
            // Only the fields the current shape actually shows count towards "the user is done
            // typing" - e.g. a Circle only has field1, so field2/field3 being blank must not count
            // against it (and Triangle needs all three filled before flagging anything).
            val requiredFieldsFilled = when (selectedShape) {
                Shape.CIRCLE -> field1.isNotBlank()
                Shape.RECTANGLE -> field1.isNotBlank() && field2.isNotBlank()
                Shape.TRIANGLE -> field1.isNotBlank() && field2.isNotBlank() && field3.isNotBlank()
            }
            if (result != null) {
                Spacer(Modifier.height(20.dp))
                Text(context.getString(R.string.calc_1_s_results, context.getString(selectedShape.label)), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val summary = context.getString(R.string.calc_1_s_n_2_s_nmeasurements_use_a_common_input_unit, context.getString(selectedShape.label), result)
                ResultCard(text = result, textStyle = MaterialTheme.typography.headlineSmall,
                    onCopy = { context.copyToClipboard(context.getString(R.string.calc_geometry), summary) },
                    onShare = { context.shareText(summary) })
            } else if (requiredFieldsFilled) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = context.getString(R.string.calc_enter_valid_positive_measurements) + if (selectedShape == Shape.TRIANGLE) context.getString(R.string.calc_that_form_a_valid_triangle) else ".",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(8.dp))
            ToolActions(onClear = { field1 = ""; field2 = ""; field3 = "" },
                clearEnabled = field1.isNotEmpty() || field2.isNotEmpty() || field3.isNotEmpty())
        })
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    val context = LocalContext.current
    DecimalField(value = value, onValueChange = onValueChange, label = label,
        placeholder = context.getString(R.string.calc_e_g_5), errorText = if (value.isNotBlank() &&
            (value.toDoubleOrNull()?.let { it.isFinite() && it > 0 } != true)) context.getString(R.string.calc_enter_a_positive_measurement) else null)

}

private fun computeResult(context: android.content.Context, shape: Shape, f1: String, f2: String, f3: String): String? {
    fun fmt(value: Double) = formatTrimmed(value, decimals = 4)

    return when (shape) {
        Shape.CIRCLE -> {
            val r = f1.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
            if (r <= 0) return null
            val area = PI * r * r
            val circumference = 2 * PI * r
            if (!area.isFinite() || area <= 0 || !circumference.isFinite()) return null
            context.getString(R.string.calc_area_1_s_square_units_ncircumference_2_s_units, fmt(area), fmt(circumference))
        }

        Shape.RECTANGLE -> {
            val w = f1.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
            val h = f2.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
            if (w <= 0 || h <= 0) return null
            val area = w * h
            val perimeter = 2 * (w + h)
            if (!area.isFinite() || area <= 0 || !perimeter.isFinite()) return null
            context.getString(R.string.calc_area_1_s_square_units_nperimeter_2_s_units, fmt(area), fmt(perimeter))
        }

        Shape.TRIANGLE -> {
            val a = f1.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
            val b = f2.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
            val c = f3.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
            if (a <= 0 || b <= 0 || c <= 0) return null
            if (a + b <= c || a + c <= b || b + c <= a) return null
            val perimeter = a + b + c
            val s = perimeter / 2
            val area = sqrt(s * (s - a) * (s - b) * (s - c))
            if (!area.isFinite() || area <= 0 || !perimeter.isFinite()) return null
            context.getString(R.string.calc_area_1_s_square_units_nperimeter_2_s_units, fmt(area), fmt(perimeter))
        }
    }
}
