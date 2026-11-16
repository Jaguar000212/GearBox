package com.jaguar.gearbox.data

import org.json.JSONArray
import org.json.JSONObject

/** Strict, versioned snapshots. Reject the whole value rather than partially restoring a match. */
object ScoreSnapshots {
    private const val MAX_PLAYERS = 100
    private const val MAX_NAME = 200

    fun scoreboard(players: List<ScorePlayer>): Map<String, String>? {
        if (players.size > MAX_PLAYERS || players.any { it.name.length > MAX_NAME }) return null
        val rows = JSONArray()
        players.forEach { rows.put(JSONObject().put("name", it.name).put("score", it.score)) }
        return mapOf("version" to "1", "players" to rows.toString()).takeIf(ToolMemoryPolicy::validInputs)
    }

    fun readScoreboard(values: Map<String, String>): List<ScorePlayer>? = runCatching {
        require(values.keys == setOf("version", "players") && values["version"] == "1")
        require(ToolMemoryPolicy.validInputs(values))
        val rows = JSONArray(values.getValue("players"))
        require(rows.length() <= MAX_PLAYERS)
        List(rows.length()) { index ->
            val row = rows.getJSONObject(index)
            require(row.length() == 2 && row.get("name") is String)
            val name = row.getString("name")
            val raw = row.get("score")
            require(raw is Int || raw is Long)
            val score = raw.toString().toInt()
            require(name.length <= MAX_NAME)
            ScorePlayer(name, score)
        }
    }.getOrNull()

    fun chess(match: ChessMatch): Map<String, String>? {
        if (!validChess(match)) return null
        return mapOf("version" to "1", "whiteName" to match.whiteName, "blackName" to match.blackName,
            "whiteScore" to match.whiteScore.toString(), "blackScore" to match.blackScore.toString(), "games" to match.gamesPlayed.toString())
    }

    fun readChess(values: Map<String, String>): ChessMatch? = runCatching {
        require(values.keys == setOf("version", "whiteName", "blackName", "whiteScore", "blackScore", "games"))
        require(values["version"] == "1" && ToolMemoryPolicy.validInputs(values))
        ChessMatch(values.getValue("whiteName"), values.getValue("blackName"),
            values.getValue("whiteScore").toFloat(), values.getValue("blackScore").toFloat(),
            values.getValue("games").toInt()).also { require(validChess(it)) }
    }.getOrNull()

    private fun validChess(match: ChessMatch): Boolean = with(match) {
        whiteName.length <= MAX_NAME && blackName.length <= MAX_NAME && gamesPlayed in 0..1_000_000 &&
            whiteScore.isFinite() && blackScore.isFinite() && whiteScore >= 0f && blackScore >= 0f &&
            whiteScore * 2 == (whiteScore * 2).toInt().toFloat() &&
            blackScore * 2 == (blackScore * 2).toInt().toFloat() &&
            whiteScore.toDouble() + blackScore.toDouble() == gamesPlayed.toDouble()
    }
}
