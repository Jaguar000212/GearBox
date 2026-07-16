package com.jaguar.gearbox.ui.tools

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.io.FileOutputStream

@Composable
fun QrGeneratorScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }

    val matrix = remember(input) { generateQrMatrix(input.trim()) }
    val bitmap = remember(matrix) { matrix?.let { matrixToBitmap(it) } }

    ToolScaffold(
        title = "QR Code Generator",
        icon = Tools.byRoute(Tools.ROUTE_QR_GENERATOR)!!.icon,
        onNavigateBack = onNavigateBack,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Text or URL") },
            placeholder = { Text("e.g. https://example.com") },
            modifier = Modifier.fillMaxWidth(),
        )

        if (bitmap != null) {
            Spacer(Modifier.height(20.dp))
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Generated QR code",
                modifier = Modifier
                    .fillMaxWidth()
                    .size(240.dp),
                alignment = Alignment.Center,
            )

            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { shareQrBitmap(context, bitmap) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Text(" Share QR code")
            }
        } else if (input.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text("Couldn't generate a QR code for that input.", color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun matrixToBitmap(matrix: com.google.zxing.common.BitMatrix): Bitmap {
    val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
    for (x in 0 until matrix.width) {
        for (y in 0 until matrix.height) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bitmap
}

private fun shareQrBitmap(context: android.content.Context, bitmap: Bitmap) {
    val qrDir = File(context.cacheDir, "qr").apply { mkdirs() }
    val file = File(qrDir, "qr_share.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
