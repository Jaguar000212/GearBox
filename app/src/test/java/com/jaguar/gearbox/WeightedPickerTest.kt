package com.jaguar.gearbox
import com.jaguar.gearbox.logic.*
import com.jaguar.gearbox.data.*
import org.junit.Assert.*
import org.junit.Test
class WeightedPickerTest {
    @Test fun exactTicketIntervalsAndBounds() {
        val weights=listOf(1,3,2)
        assertEquals(listOf(0,1,1,1,2,2),(0 until 6).map {weightedPickerIndex(weights,it)})
        assertEquals(2000000,weightedTotal(List(200){10000}))
        assertNull(weightedTotal(List(201){1}));assertNull(weightedTotal(listOf(0)));assertNull(weightedTotal(listOf(10001)))
        for(value in listOf("0","01","-1","1.0","10001")) assertNull(parsePickerWeight(value))
        assertTrue(validWeightedName("😀".repeat(120)));assertFalse(validWeightedName("😀".repeat(121)))
        assertFalse(validWeightedName("a\u2028b"));assertFalse(validWeightedName("\uD800"))
    }
    @Test fun strictRestorePreservesLegacyShapeAndDuplicateIdentity() {
        val legacy=mapOf("participants" to "A\nA","mode" to "weighted","teams" to "2")
        val restored=parseWeightedPickerSnapshot(legacy)!!
        assertEquals(listOf(1,1),restored.weights);assertEquals(legacy,restored.toInputs())
        val weighted=legacy+mapOf("weights" to "1\n3")
        assertEquals(weighted,parseWeightedPickerSnapshot(weighted)!!.toInputs())
        assertNull(parseWeightedPickerSnapshot(weighted+mapOf("weights" to "01\n3")))
        assertNull(parseWeightedPickerSnapshot(weighted+mapOf("teams" to Int.MAX_VALUE.toString())))
        assertNull(parseWeightedPickerSnapshot(weighted+mapOf("extra" to "x")))
        assertTrue(ToolMemoryPolicy.validMode("tool/team_picker",mapOf("mode" to "weighted","teams" to "2")))
        assertFalse(ToolMemoryPolicy.validMode("tool/team_picker",weighted))
    }
}
