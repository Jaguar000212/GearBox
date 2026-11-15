package com.jaguar.gearbox.ui.tools

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
import com.jaguar.gearbox.ui.components.ToolScaffold
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
    val scope = rememberCoroutineScope()

    val matrix = remember(input) { generateQrMatrix(input) }
    val bitmap = remember(matrix) { matrix?.let { matrixToBitmap(it) } }

    ToolScaffold(
        title = "QR Code Generator",
        icon = Tools.byRoute(Tools.ROUTE_QR_GENERATOR)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; shareError = null },
            label = { Text("Text or URL") },
            placeholder = { Text("e.g. https://example.com") },
            readOnly = sharing,
            supportingText = { Text("Your code updates as you type. Text and links are both supported.") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("QR preview", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                when {
                    bitmap != null -> {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR code containing your entered text",
                            modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth().aspectRatio(1f),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Ready to scan or share", style = MaterialTheme.typography.bodyMedium)
                    }
                    input.isEmpty() -> {
                        Icon(Tools.byRoute(Tools.ROUTE_QR_GENERATOR)!!.icon, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Your QR code will appear here", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Enter text or a link above to get started.")
                    }
                    else -> Text("Couldn't generate a QR code. Try shorter text.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (bitmap != null) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    // Lock the input before starting IO so the preview and shared payload agree.
                    sharing = true
                    shareError = null
                    scope.launch {
                        try {
                            shareError = shareQrBitmap(context, bitmap)
                        } finally {
                            sharing = false
                        }
                    }
                },
                enabled = !sharing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Text(if (sharing) " Preparing image…" else " Share QR image")
            }
        }
        shareError?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
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
private suspend fun shareQrBitmap(context: android.content.Context, bitmap: Bitmap): String? {
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
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "QR code", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
        null
    } catch (_: ActivityNotFoundException) {
        "No app is available to share this QR code."
    } catch (_: IOException) {
        "Couldn't save the QR image. Check available storage and try again."
    } catch (_: SecurityException) {
        "Couldn't grant access to the QR image. Please try again."
    } catch (_: IllegalArgumentException) {
        "Couldn't prepare the QR image for sharing."
    }
}
