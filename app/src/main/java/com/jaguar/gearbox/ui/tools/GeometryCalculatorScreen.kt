package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.jaguar.gearbox.ui.components.ToolScaffold
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sqrt

private enum class Shape(val label: String) {
    CIRCLE("Circle"),
    RECTANGLE("Rectangle"),
    TRIANGLE("Triangle"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeometryCalculatorScreen(onNavigateBack: () -> Unit) {
    var shape by rememberSaveable { mutableStateOf(Shape.CIRCLE.name) }
    var shapeExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedShape = Shape.valueOf(shape)

    var field1 by rememberSaveable(selectedShape) { mutableStateOf("") }
    var field2 by rememberSaveable(selectedShape) { mutableStateOf("") }
    var field3 by rememberSaveable(selectedShape) { mutableStateOf("") }

    ToolScaffold(
        title = "Geometry Calculator",
        icon = Tools.byRoute(Tools.ROUTE_GEOMETRY)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        ExposedDropdownMenuBox(
            expanded = shapeExpanded,
            onExpandedChange = { shapeExpanded = it },
        ) {
            OutlinedTextField(
                value = selectedShape.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Shape") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shapeExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            DropdownMenu(expanded = shapeExpanded, onDismissRequest = { shapeExpanded = false }) {
                Shape.entries.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text(entry.label) },
                        onClick = {
                            shape = entry.name
                            shapeExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        when (selectedShape) {
            Shape.CIRCLE -> {
                NumberField("Radius", field1) { field1 = it }
            }
            Shape.RECTANGLE -> {
                NumberField("Width", field1) { field1 = it }
                Spacer(Modifier.height(12.dp))
                NumberField("Height", field2) { field2 = it }
            }
            Shape.TRIANGLE -> {
                NumberField("Side A", field1) { field1 = it }
                Spacer(Modifier.height(12.dp))
                NumberField("Side B", field2) { field2 = it }
                Spacer(Modifier.height(12.dp))
                NumberField("Side C", field3) { field3 = it }
            }
        }

        val result = computeResult(selectedShape, field1, field2, field3)
        if (result != null) {
            Spacer(Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        } else if (field1.isNotBlank() || field2.isNotBlank() || field3.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Enter valid, positive measurements" + if (selectedShape == Shape.TRIANGLE) " that form a valid triangle." else ".",
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun computeResult(shape: Shape, f1: String, f2: String, f3: String): String? {
    fun fmt(value: Double) = String.format(Locale.getDefault(), "%.4f", value).trimEnd('0').trimEnd('.')

    return when (shape) {
        Shape.CIRCLE -> {
            val r = f1.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
            if (r <= 0) return null
            val area = PI * r * r
            val circumference = 2 * PI * r
            "Area: ${fmt(area)}\nCircumference: ${fmt(circumference)}"
        }
        Shape.RECTANGLE -> {
            val w = f1.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
            val h = f2.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
            if (w <= 0 || h <= 0) return null
            val area = w * h
            val perimeter = 2 * (w + h)
            "Area: ${fmt(area)}\nPerimeter: ${fmt(perimeter)}"
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
            "Area: ${fmt(area)}\nPerimeter: ${fmt(perimeter)}"
        }
    }
}
