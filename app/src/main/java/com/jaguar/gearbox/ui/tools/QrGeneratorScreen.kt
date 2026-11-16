package com.jaguar.gearbox.ui.tools

import com.jaguar.gearbox.R
import android.content.Intent
import android.content.ClipData
import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.TextButton
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.jaguar.gearbox.data.Tools
import com.jaguar.gearbox.logic.generateQrMatrix
import com.jaguar.gearbox.ui.components.SensitiveTextShareDialog
import com.jaguar.gearbox.ui.components.ToolScaffold
import com.jaguar.gearbox.ui.components.rememberToolHistory
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QrGeneratorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var shareError by remember { mutableStateOf<String?>(null) }
    var sharing by remember { mutableStateOf(false) }
    var shareReview by remember { mutableStateOf<Pair<String, Bitmap>?>(null) }
    val scope = rememberCoroutineScope()

    var restoredInput by rememberSaveable { mutableStateOf(false) }
    var renderedInput by remember { mutableStateOf<String?>(null) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(input, restoredInput) {
        val requestedInput = input
        renderedInput = null
        renderedBitmap = null
        if (requestedInput.isNotEmpty() && !restoredInput) {
            delay(150)
            val result = withContext(Dispatchers.Default) {
                generateQrMatrix(requestedInput)?.let { matrixToBitmap(it) }
            }
            renderedBitmap = result
        }
        renderedInput = requestedInput
    }
    // Never display or share an obsolete payload while the latest input is processing.
    val bitmap = renderedBitmap.takeIf { renderedInput == input }
    val generating = !restoredInput && input.isNotEmpty() && renderedInput != input

    val history = rememberToolHistory(Tools.ROUTE_QR_GENERATOR,
        inputs = mapOf("text" to input), output = if (bitmap != null) context.getString(R.string.special_qr_content, input) else null,
        resultRequiresExplicitAction = true,
        onRestore = { values ->
            input = values["text"].orEmpty(); restoredInput = true
            renderedInput = null; renderedBitmap = null; shareError = null
        })

    ToolScaffold(
        title = context.getString(R.string.special_qr_code_generator),
        icon = Tools.byRoute(Tools.ROUTE_QR_GENERATOR)!!.icon,
        onNavigateBack = onNavigateBack,
        history = history,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; shareError = null; restoredInput = false },
            label = { Text(context.getString(R.string.special_text_or_url)) },
            placeholder = { Text(context.getString(R.string.special_e_g_https_example_com)) },
            readOnly = sharing,
            maxLines = 6,
            supportingText = { Text(context.getString(R.string.special_your_code_updates_as_you_type_text_and_links_are_both_supported)) },
            modifier = Modifier.fillMaxWidth(),
        )

        TextButton(onClick = { input = ""; shareError = null; restoredInput = false }, enabled = input.isNotEmpty() && !sharing) { Text(context.getString(R.string.screen_clear)) }
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(context.getString(R.string.special_qr_preview), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                when {
                    restoredInput && input.isNotEmpty() -> {
                        Text(context.getString(R.string.special_inputs_restored_generate_a_fresh_code_when_you_are_ready))
                        Button(onClick = { restoredInput = false }) { Text(context.getString(R.string.special_generate_qr_code)) }
                    }
                    generating -> Text(context.getString(R.string.special_generating_qr_code))
                    bitmap != null -> {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = context.getString(R.string.special_qr_code_containing_your_entered_text),
                            modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(context.getString(R.string.special_ready_to_scan_or_share), style = MaterialTheme.typography.bodyMedium)
                    }
                    input.isEmpty() -> {
                        Icon(Tools.byRoute(Tools.ROUTE_QR_GENERATOR)!!.icon, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(context.getString(R.string.special_your_qr_code_will_appear_here), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(context.getString(R.string.special_enter_text_or_a_link_above_to_get_started))
                    }
                    else -> Text(context.getString(R.string.special_couldn_t_generate_a_qr_code_try_shorter_text), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (bitmap != null) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    // Reviewing does not write an image or launch an external application.
                    if (!sharing && !restoredInput && renderedInput == input)
                        shareReview = input to bitmap
                },
                enabled = !sharing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Text(if (sharing) context.getString(R.string.special_preparing_image) else context.getString(R.string.special_share_qr_image))
            }
        }
        shareError?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
    }
    shareReview?.let { snapshot ->
        val isCurrent = !restoredInput && input == snapshot.first && renderedInput == snapshot.first && renderedBitmap === snapshot.second
        if (!isCurrent) {
            LaunchedEffect(snapshot) { shareReview = null }
        } else SensitiveTextShareDialog(
            text = snapshot.first,
            disclosure = context.getString(R.string.special_the_external_app_you_choose_will_receive_a_qr_image_encoding_the_),
            previewLabel = context.getString(R.string.special_content_encoded_in_the_qr_image),
            onDismiss = { shareReview = null },
            onConfirm = {
                shareReview = null
                // Check again at confirmation; keep the original lock and final payload guard.
                if (!sharing && !restoredInput && input == snapshot.first && renderedInput == snapshot.first && renderedBitmap === snapshot.second) {
                    sharing = true
                    shareError = null
                    scope.launch {
                        try {
                            shareError = shareQrBitmap(context, snapshot.second) {
                                !restoredInput && input == snapshot.first && renderedInput == snapshot.first && renderedBitmap === snapshot.second
                            }
                        } finally { sharing = false }
                    }
                }
            },
        )
    }
}

internal fun matrixToBitmap(matrix: com.google.zxing.common.BitMatrix): Bitmap {
    val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
    for (x in 0 until matrix.width) {
        for (y in 0 until matrix.height) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bitmap
}

/** Returns an actionable error instead of letting storage/intent failures crash the screen. */
private suspend fun shareQrBitmap(context: android.content.Context, bitmap: Bitmap, isCurrent: () -> Boolean): String? {
    return try {
        val uri = withContext(Dispatchers.IO) {
            val qrDir = File(context.cacheDir, "qr")
            prepareQrShareFile(qrDir, write = { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IOException("PNG encoding failed")
                }
            }, prepare = { file ->
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            })
        }
        if (!isCurrent()) return context.getString(R.string.special_qr_content_changed_generate_the_updated_code_before_sharing)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, context.getString(R.string.special_qr_code), uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
        null
    } catch (_: ActivityNotFoundException) {
        context.getString(R.string.special_no_app_is_available_to_share_this_qr_code)
    } catch (_: IOException) {
        context.getString(R.string.special_couldn_t_save_the_qr_image_check_available_storage_and_try_again)
    } catch (_: SecurityException) {
        context.getString(R.string.special_couldn_t_grant_access_to_the_qr_image_please_try_again)
    } catch (_: IllegalArgumentException) {
        context.getString(R.string.special_couldn_t_prepare_the_qr_image_for_sharing)
    }
}
