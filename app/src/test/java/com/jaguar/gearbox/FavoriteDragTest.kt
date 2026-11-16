package com.jaguar.gearbox

import com.jaguar.gearbox.logic.favoriteDragTranslation
import com.jaguar.gearbox.logic.favoriteEdgeScrollVelocity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteDragTest {
    @Test fun edgeHoldHasVelocityWithoutNewPointerMovement() {
        repeat(10) {
            assertEquals(240f, favoriteEdgeScrollVelocity(568f, 0f, 600f, 64f, 480f), 0.01f)
            assertEquals(-240f, favoriteEdgeScrollVelocity(32f, 0f, 600f, 64f, 480f), 0.01f)
        }
        assertEquals(0f, favoriteEdgeScrollVelocity(300f, 0f, 600f, 64f, 480f), 0f)
    }
    @Test fun translatedCenterStaysFixedAcrossScrollAndReorder() {
        val center = 520f
        listOf(100, 82, 250, 232).forEach { offset ->
            assertEquals(center, offset + 60 + favoriteDragTranslation(center, offset, 120), 0f)
        }
    }
    @Test fun speedIsBoundedAndShortViewportsRemainValid() {
        assertEquals(480f, favoriteEdgeScrollVelocity(800f, 0f, 600f, 64f, 480f), 0f)
        assertEquals(0f, favoriteEdgeScrollVelocity(1f, 0f, 0f, 64f, 480f), 0f)
        assertTrue(favoriteEdgeScrollVelocity(95f, 0f, 100f, 64f, 480f) in 0f..480f)
    }
}
