package com.jaguar.gearbox.logic

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Pure ZXing-core call (no Android dependency, hence JVM-testable) - encoding is separated from
 * the Bitmap conversion, which needs android.graphics and lives in the screen composable instead.
 */
fun generateQrMatrix(text: String, size: Int = 512): BitMatrix? {
    if (text.isEmpty()) return null
    return runCatching {
        QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
    }.getOrNull()
}
