package com.jaguar.gearbox.logic

/** Pixels/second. Frame-driven scrolling continues while the pointer is held at an edge. */
fun favoriteEdgeScrollVelocity(center: Float, start: Float, end: Float, edge: Float, maxSpeed: Float): Float {
    if (!center.isFinite() || edge <= 0f || end <= start || maxSpeed <= 0f) return 0f
    val zone = edge.coerceAtMost((end - start) / 2f)
    return when {
        center < start + zone -> -maxSpeed * ((start + zone - center) / zone).coerceIn(0f, 1f)
        center > end - zone -> maxSpeed * ((center - (end - zone)) / zone).coerceIn(0f, 1f)
        else -> 0f
    }
}

/** Keep the dragged card under the pointer as both scrolling and item reordering move its slot. */
fun favoriteDragTranslation(center: Float, itemOffset: Int, itemSize: Int): Float = center - itemOffset - itemSize / 2f
