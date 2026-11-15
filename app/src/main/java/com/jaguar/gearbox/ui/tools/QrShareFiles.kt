package com.jaguar.gearbox.ui.tools

import java.io.File
import java.io.IOException
import java.io.OutputStream

/**
 * Shared images are temporary cache files, not saved exports. Keep completed shares for seven
 * days so opening another chooser never invalidates a recent recipient's URI. Android may
 * evict cache sooner; recipients needing permanent access must copy the image. A chooser does
 * not tell us when a recipient finishes reading, so cleanup is age-based, not immediate.
 */
internal const val QR_SHARE_RETENTION_MS = 7L * 24 * 60 * 60 * 1000

internal fun cleanupStaleQrShares(directory: File, now: Long = System.currentTimeMillis()) {
    directory.listFiles()?.forEach { file ->
        val modified = file.lastModified()
        if (file.isFile && file.name.startsWith("qr_share_") && file.name.endsWith(".png") &&
            modified > 0 && modified < now && now - modified > QR_SHARE_RETENTION_MS
        ) {
            // Best effort: failure to remove an old cache file must not prevent a new share.
            file.delete()
        }
    }
}

/** Writes a fresh, immutable share target; removes failed/unpublished files even on exceptions. */
internal fun <T> prepareQrShareFile(
    directory: File,
    write: (OutputStream) -> Unit,
    prepare: (File) -> T,
): T {
    if (!directory.isDirectory && !directory.mkdirs()) throw IOException("QR cache unavailable")
    cleanupStaleQrShares(directory)
    val file = File.createTempFile("qr_share_", ".png", directory)
    var prepared = false
    try {
        file.outputStream().use(write)
        val result = prepare(file)
        prepared = true
        return result
    } finally {
        if (!prepared) file.delete()
    }
}
