package com.jaguar.gearbox.images

import android.net.Uri
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** Request identity and editor generation are separate; cancel cannot validate a later callback. */
data class StudioReplacementRequest(val id: Long, val session: Long, val collage: Boolean)
class StudioReplacementGate {
    private var serial = 0L
    private var pending: StudioReplacementRequest? = null
    fun begin(session: Long, collage: Boolean): StudioReplacementRequest =
        StudioReplacementRequest(++serial, session, collage).also { pending = it }
    fun current(request: StudioReplacementRequest, session: Long, collage: Boolean): Boolean =
        pending === request && request.session == session && request.collage == collage
    fun finish(request: StudioReplacementRequest): Boolean =
        (pending === request).also { if (it) pending = null }
    fun cancel() { pending = null }
}

/** Owns every unpublished bitmap, including partial batches. The caller owns the successful list.
 * The loader must transfer ownership only on successful return, as loadStudioImage does. */
suspend fun loadStudioReplacement(uris: List<Uri>, pixelLimit: Long,
    loader: suspend (Uri, Long) -> ImageStudioImage): List<ImageStudioImage> {
    require(uris.size in 1..4)
    val loaded = ArrayList<ImageStudioImage>(uris.size)
    try {
        for (uri in uris) {
            coroutineContext.ensureActive()
            val image = loader(uri, pixelLimit)
            // Keep ownership before any further cancellation check or fallible allocation.
            try { loaded.add(image) } catch (error: Throwable) { image.bitmap.recycle(); throw error }
            coroutineContext.ensureActive()
        }
        return loaded
    } catch (error: Throwable) {
        loaded.forEach { if (!it.bitmap.isRecycled) it.bitmap.recycle() }
        throw error
    }
}
