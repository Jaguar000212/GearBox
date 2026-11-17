package com.jaguar.gearbox.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.jaguar.gearbox.R
import com.jaguar.gearbox.images.displayedPixel
import com.jaguar.gearbox.images.loadLocalSamplingImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Transient selected image only. The callback contains RGB, never URI/bytes/image metadata. */
@Composable
fun LocalImageColorSampler(onColorPicked: (red: Int, green: Int, blue: Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val applyColor by rememberUpdatedState(onColorPicked)
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    var generation by remember { mutableLongStateOf(0L) }
    var disposed by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var x by remember { mutableStateOf("0") }
    var y by remember { mutableStateOf("0") }
    var sampled by remember { mutableStateOf(false) }
    var displaySize by remember { mutableStateOf(IntSize.Zero) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        picking = false
        if (uri != null && !disposed) {
            val token = generation
            loading = true
            job = scope.launch {
                try {
                    val decoded = loadLocalSamplingImage(context.contentResolver, uri)
                    if (!disposed && token == generation) {
                        bitmap = decoded; x = "0"; y = "0"; sampled = false
                    } else decoded.recycle()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { if (!disposed && token == generation) error = true }
                catch (_: OutOfMemoryError) { if (!disposed && token == generation) error = true }
                finally { if (!disposed && token == generation) loading = false }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { disposed = true; generation++; job?.cancel() } }
    fun discard() {
        generation++; job?.cancel(); job = null
        // Do not recycle a bitmap still referenced by a Compose drawing frame.
        bitmap = null; loading = false; error = false; sampled = false
    }
    fun sample(image: Bitmap, column: Int, row: Int) {
        if (image !== bitmap || loading || disposed || column !in 0 until image.width || row !in 0 until image.height) return
        val color = image.getPixel(column, row)
        x = column.toString(); y = row.toString(); sampled = true
        applyColor(android.graphics.Color.red(color), android.graphics.Color.green(color), android.graphics.Color.blue(color))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(context.getString(R.string.image_sampler_title), style = MaterialTheme.typography.titleMedium)
        Text(context.getString(R.string.image_sampler_disclosure), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(enabled = !picking, onClick = {
            discard()
            picking = true
            runCatching { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                .onFailure { picking = false; error = true }
        }) { Text(context.getString(R.string.image_sampler_choose)) }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error) Text(context.getString(R.string.image_sampler_error), color = MaterialTheme.colorScheme.error)
        bitmap?.let { image ->
            Text(context.getString(R.string.image_sampler_size, image.width, image.height), style = MaterialTheme.typography.bodySmall)
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val ratio = image.width.toFloat() / image.height
            val previewWidth = minOf(maxWidth, 320.dp * ratio)
            Image(image.asImageBitmap(), contentDescription = context.getString(R.string.image_sampler_preview),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.width(previewWidth).aspectRatio(ratio)
                    .onSizeChanged { displaySize = it }
                    .pointerInput(image) { detectTapGestures { tap ->
                        displayedPixel(tap.x, tap.y, displaySize.width, displaySize.height, image.width, image.height)
                            ?.let { (column, row) -> sample(image, column, row) }
                    } })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(x, { x = it; sampled = false }, label = { Text(context.getString(R.string.image_sampler_x)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(y, { y = it; sampled = false }, label = { Text(context.getString(R.string.image_sampler_y)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            }
            val column = x.toIntOrNull()
            val row = y.toIntOrNull()
            Button(enabled = column != null && row != null && column in 0 until image.width && row in 0 until image.height,
                onClick = { if (column != null && row != null) sample(image, column, row) }) {
                Text(context.getString(R.string.image_sampler_sample))
            }
            if (sampled) Text(context.getString(R.string.image_sampler_applied, x, y), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { discard() }) { Text(context.getString(R.string.image_sampler_remove)) }
        }
    }
}
