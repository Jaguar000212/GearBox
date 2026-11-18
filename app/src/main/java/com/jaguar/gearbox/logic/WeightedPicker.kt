package com.jaguar.gearbox.logic

import kotlin.random.Random
fun validWeightedName(value: String): Boolean {
    if(value.isBlank() || value.length > 240 || value.codePointCount(0,value.length)>120) return false
    var i=0
    while(i<value.length) {
        val c=value[i]
        if(c.isISOControl() || c=='\u2028' || c=='\u2029') return false
        if(c.isHighSurrogate()) { if(i+1>=value.length || !value[i+1].isLowSurrogate()) return false; i++ }
        else if(c.isLowSurrogate()) return false
        i++
    }
    return true
}
fun parsePickerWeight(value: String): Int? = if(Regex("[1-9][0-9]{0,4}").matches(value)) value.toIntOrNull()?.takeIf {it in 1..10000} else null
fun weightedTotal(weights: List<Int>): Int? = if(weights.size in 1..200 && weights.all {it in 1..10000}) weights.sum() else null
fun weightedPickerIndex(weights: List<Int>, ticket: Int): Int {
    val total=requireNotNull(weightedTotal(weights))
    require(ticket in 0 until total)
    var end=0
    for((index,weight) in weights.withIndex()) {end+=weight;if(ticket<end) return index}
    error("Unreachable ticket")
}
fun pickWeightedIndex(weights: List<Int>, random: Random=Random.Default): Int =
    weightedPickerIndex(weights,random.nextInt(requireNotNull(weightedTotal(weights))))
