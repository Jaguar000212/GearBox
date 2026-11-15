package com.jaguar.gearbox.ui.tools

import java.io.File
import java.io.IOException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class QrShareFilesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun cleanupOnlyRemovesOwnedFilesOlderThanRetention() {
        val directory = temporary.newFolder()
        val now = 2 * QR_SHARE_RETENTION_MS
        fun file(name: String, modified: Long) = File(directory, name).apply {
            writeText(name)
            assertTrue(setLastModified(modified))
        }
        val stale = file("qr_share_stale.png", now - QR_SHARE_RETENTION_MS - 1)
        val boundary = file("qr_share_boundary.png", now - QR_SHARE_RETENTION_MS)
        val recent = file("qr_share_recent.png", now - 1)
        val future = file("qr_share_future.png", now + 1)
        val unrelated = file("other.png", 1)
        val unknown = file("qr_share_unknown.png", 0)
        val nested = File(directory, "qr_share_directory.png").apply { mkdir(); setLastModified(1) }
        cleanupStaleQrShares(directory, now)
        assertFalse(stale.exists())
        for (kept in listOf(boundary, recent, future, unrelated, unknown, nested)) assertTrue(kept.exists())
    }

    @Test fun subsequentSharesPreserveEarlierFileAndPayload() {
        val directory = temporary.newFolder()
        fun share(text: String) = prepareQrShareFile(directory, { it.write(text.toByteArray()) }, { it })
        val first = share("first payload")
        val second = share("second payload")
        assertNotEquals(first, second)
        cleanupStaleQrShares(directory, first.lastModified() + QR_SHARE_RETENTION_MS)
        assertEquals("first payload", first.readText())
        assertEquals("second payload", second.readText())
    }

    @Test fun writeAndPreparationFailuresDoNotLeaveOrphans() {
        val directory = temporary.newFolder()
        for (failWrite in listOf(true, false)) {
            try {
                prepareQrShareFile(directory, {
                    it.write(1)
                    if (failWrite) throw IOException("write failed")
                }, { throw IllegalArgumentException("URI preparation failed") })
                fail("Expected preparation to fail")
            } catch (expected: IOException) {
                assertTrue(failWrite)
            } catch (expected: IllegalArgumentException) {
                assertFalse(failWrite)
            }
            assertTrue(directory.listFiles()!!.isEmpty())
        }
    }
}
