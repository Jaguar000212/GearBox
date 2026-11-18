package com.jaguar.gearbox.data
import com.jaguar.gearbox.logic.*
data class WeightedPickerSnapshot(val participants: List<String>, val weights: List<Int>, val teams: Int, val includesWeights: Boolean=true) {
    fun toInputs(): Map<String,String> = buildMap {
        put("participants",participants.joinToString("\n"));put("mode","weighted");put("teams",teams.toString())
        if(includesWeights) put("weights",weights.joinToString("\n"))
    }
}
fun parseWeightedPickerSnapshot(values: Map<String,String>): WeightedPickerSnapshot? {
    if(values.keys != setOf("participants","mode","teams") && values.keys != setOf("participants","mode","teams","weights")) return null
    if(values["mode"]!="weighted") return null
    val raw=values["participants"] ?: return null
    if(raw.length>48199) return null
    val names=raw.split('\n')
    if(names.size !in 1..200 || names.any {!validWeightedName(it)}) return null
    val teams=values["teams"]?.toIntOrNull() ?: return null
    if(teams !in 2..names.size.coerceAtLeast(2) || teams.toString()!=values["teams"]) return null
    val rawWeights=values["weights"]
    if(rawWeights != null && rawWeights.length>1199) return null
    val weights=rawWeights?.split('\n')?.map {parsePickerWeight(it) ?: return null} ?: List(names.size){1}
    if(weights.size!=names.size || weightedTotal(weights)==null) return null
    return WeightedPickerSnapshot(names,weights,teams,rawWeights!=null)
}
fun validTeamPickerMode(values: Map<String,String>): Boolean = values.keys==setOf("mode","teams") &&
    values["mode"] in setOf("pick","teams","weighted") && values["teams"]?.let {it.toIntOrNull()?.let {n -> n>=2 && n.toString()==it}}==true
