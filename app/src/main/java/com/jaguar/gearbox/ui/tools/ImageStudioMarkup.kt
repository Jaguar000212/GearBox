package com.jaguar.gearbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.jaguar.gearbox.R
import com.jaguar.gearbox.images.*
import com.jaguar.gearbox.ui.components.CompactChoiceSelector

private fun editLabel(kind: ImageStudioEditKind): Int = when (kind) {
    ImageStudioEditKind.RECTANGLE -> R.string.studio_markup_rectangle
    ImageStudioEditKind.ARROW -> R.string.studio_markup_arrow
    ImageStudioEditKind.SOLID -> R.string.studio_markup_cover
    ImageStudioEditKind.BLUR -> R.string.studio_markup_blur
    ImageStudioEditKind.MOSAIC -> R.string.studio_markup_mosaic
    ImageStudioEditKind.TEXT -> R.string.studio_text_kind
}

/** Only the explicit list is rendered. Draft changes invalidate previews but never apply an edit. */
@Composable
internal fun ImageStudioMarkup(width: Int, height: Int, edits: List<ImageStudioEdit>, enabled: Boolean,
    onDraftChanged: () -> Unit, onEditsChanged: (List<ImageStudioEdit>) -> Unit) {
    val context = LocalContext.current
    var annotation by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var kind by remember { mutableStateOf(ImageStudioEditKind.RECTANGLE) }
    var x1 by remember { mutableStateOf("0") }; var y1 by remember { mutableStateOf("0") }
    var x2 by remember { mutableStateOf(width.toString()) }; var y2 by remember { mutableStateOf(height.toString()) }
    var color by remember { mutableStateOf("#000000") }; var strength by remember { mutableStateOf("8") }
    fun number(s: String): Int? = s.takeIf { Regex("0|[1-9][0-9]{0,4}").matches(it) }?.toIntOrNull()
    val values = listOf(x1, y1, if(kind == ImageStudioEditKind.TEXT) width.toString() else x2, if(kind == ImageStudioEditKind.TEXT) height.toString() else y2).map(::number)
    val usesColor = kind in listOf(ImageStudioEditKind.RECTANGLE, ImageStudioEditKind.ARROW, ImageStudioEditKind.SOLID, ImageStudioEditKind.TEXT)
    val parsedColor = if (!usesColor) 0xff000000.toInt() else color.takeIf { Regex("#[0-9a-fA-F]{6}").matches(it) }
        ?.drop(1)?.toLongOrNull(16)?.or(0xff000000L)?.toInt()
    val size = if (kind == ImageStudioEditKind.SOLID) 8 else number(strength)
    val draft = if (values.all { it != null } && parsedColor != null && size != null)
        ImageStudioEdit(kind, ImageStudioCrop(values[0]!!, values[1]!!, values[2]!!, values[3]!!), parsedColor, size, if (kind == ImageStudioEditKind.TEXT) annotation else "")
            .takeIf { it.validFor(width, height) } else null
    Text(stringResource(R.string.studio_markup_count, edits.size))
    edits.forEachIndexed { index, edit ->
        Text(stringResource(R.string.studio_markup_item, index + 1, context.getString(editLabel(edit.kind)),
            edit.region.left, edit.region.top, edit.region.right, edit.region.bottom))
        Text(stringResource(R.string.studio_markup_parameters,
            "#" + (edit.color.toLong() and 0xffffffL).toString(16).uppercase().padStart(6, '0'), edit.strength))
        if (edit.kind == ImageStudioEditKind.TEXT) Text(stringResource(R.string.studio_text_value, edit.text))
        TextButton(enabled = enabled, onClick = { onEditsChanged(edits.filterIndexed { i, _ -> i != index }) }) {
            Text(stringResource(R.string.studio_markup_delete, index + 1))
        }
    }
    if (edits.isNotEmpty()) TextButton(enabled = enabled, onClick = { onEditsChanged(emptyList()) }) {
        Text(stringResource(R.string.studio_markup_clear))
    }
    OutlinedButton(enabled = enabled, onClick = { expanded = !expanded }) { Text(stringResource(R.string.studio_markup_show)) }
    if (expanded) {
        Text(stringResource(R.string.studio_markup_title), style = MaterialTheme.typography.titleMedium)
        CompactChoiceSelector(stringResource(R.string.studio_markup_kind), kind, ImageStudioEditKind.entries,
            { context.getString(editLabel(it)) }, { if (enabled) {
                if (it == ImageStudioEditKind.ARROW && kind != it) {
                    if (x2 == width.toString()) x2 = (width - 1).toString()
                    if (y2 == height.toString()) y2 = (height - 1).toString()
                }
                kind = it; onDraftChanged()
            } })
        listOf(Triple(x1, R.string.studio_markup_x1, { v: String -> x1 = v }),
            Triple(y1, R.string.studio_markup_y1, { v: String -> y1 = v }),
            Triple(x2, R.string.studio_markup_x2, { v: String -> x2 = v }),
            Triple(y2, R.string.studio_markup_y2, { v: String -> y2 = v })).let { if(kind == ImageStudioEditKind.TEXT) it.take(2) else it }.forEach { (value, label, change) ->
            OutlinedTextField(value, { change(it); onDraftChanged() }, enabled = enabled, singleLine = true,
                label = { Text(stringResource(label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth())
        }
        if (kind == ImageStudioEditKind.TEXT) {
            OutlinedTextField(annotation, { if(it.length <= 240) { annotation = it; onDraftChanged() } }, enabled = enabled, singleLine = true,
                label = { Text(stringResource(R.string.studio_text_input)) }, isError = !validStudioAnnotation(annotation), modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.studio_text_note), style = MaterialTheme.typography.bodySmall)
        }
        if (usesColor) OutlinedTextField(color, { color = it; onDraftChanged() }, enabled = enabled,
            singleLine = true, label = { Text(stringResource(R.string.studio_markup_color)) }, modifier = Modifier.fillMaxWidth())
        if (kind != ImageStudioEditKind.SOLID) OutlinedTextField(strength, { strength = it; onDraftChanged() }, enabled = enabled,
            singleLine = true, label = { Text(stringResource(if(kind == ImageStudioEditKind.TEXT) R.string.studio_text_size else R.string.studio_markup_size)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.studio_markup_bounds), style = MaterialTheme.typography.bodySmall)
        if (draft == null || edits.size >= 20) Text(stringResource(R.string.studio_markup_invalid), color = MaterialTheme.colorScheme.error)
        Button(enabled = enabled && draft != null && edits.size < 20, onClick = { draft?.let { onEditsChanged(edits + it) } }) {
            Text(stringResource(R.string.studio_markup_add))
        }
    }
    Text(stringResource(R.string.studio_markup_note), style = MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.studio_markup_privacy), style = MaterialTheme.typography.bodySmall)
}
