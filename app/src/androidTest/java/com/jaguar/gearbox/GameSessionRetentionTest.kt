package com.jaguar.gearbox

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import com.jaguar.gearbox.data.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class GameSessionRetentionTest {
    private val base = InstrumentationRegistry.getInstrumentation().context
    private val id = "game_retention_${UUID.randomUUID()}"
    private val directory = File(base.cacheDir, id)
    private val context = object : ContextWrapper(base) {
        override fun getApplicationContext(): Context = this
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            base.getSharedPreferences("${id}_$name", mode)
    }
    private val memory = ToolMemoryStore.inDirectory(directory)
    private val policy = GameSessionPolicy(context, memory)
    private val prefs get() = context.getSharedPreferences("gearbox_prefs", Context.MODE_PRIVATE)
    @After fun cleanup() { directory.deleteRecursively(); base.deleteSharedPreferences("${id}_gearbox_prefs") }

    @Test fun legacySettingsDefaultToRememberingAndNewChoicePersists() {
        directory.mkdirs()
        File(directory, "settings.json").writeText("""{"version":1,"historyEnabled":false,"historyLimit":20}""")
        assertTrue(memory.settings().rememberGameSessions)
        assertTrue(memory.saveSettings(memory.settings().copy(rememberGameSessions = false)))
        assertFalse(ToolMemoryStore.inDirectory(directory).settings().rememberGameSessions)
    }

    @Test fun defaultPreservesGamesAndToggleStopsSimpleSessionReadsAndWritesOnly() {
        assertTrue(memory.settings().rememberGameSessions)
        val games = SimplePrefsStore(context, gameSession = true, sessionPolicy = policy)
        games.putString("team_picker.names", "Alex\nSam")
        games.edit { putInt("rps.wins", 4); putString("tambola.called", "1,2") }
        memory.saveSettings(memory.settings().copy(rememberGameSessions = false))
        assertEquals("", games.getString("team_picker.names", ""))
        assertEquals(0, games.getInt("rps.wins", 0))
        assertTrue(games.getIntList("tambola.called").isEmpty())
        games.putString("team_picker.names", "Temporary")
        games.edit { putInt("rps.wins", 99) }
        assertEquals("Alex\nSam", prefs.getString("team_picker.names", null))
        assertEquals(4, prefs.getInt("rps.wins", 0))
        val ordinary = SimplePrefsStore(context, sessionPolicy = policy)
        ordinary.putInt("counter.value", 7)
        assertEquals(7, ordinary.getInt("counter.value", 0))
        memory.saveSettings(memory.settings().copy(rememberGameSessions = true))
        assertEquals("Alex\nSam", games.getString("team_picker.names", ""))
    }

    @Test fun disabledScoreboardsRemainUsableWithoutReplacingSavedSession() {
        val original = ScoreboardStore(context, "scoreboard_players", policy)
        original.add(ScorePlayer("Saved", 8))
        memory.saveSettings(memory.settings().copy(rememberGameSessions = false))
        assertEquals(listOf(ScorePlayer("Saved", 8)), original.players.toList())
        original.adjustScore(0, 1)
        val temporary = ScoreboardStore(context, "scoreboard_players", policy)
        assertTrue(temporary.players.isEmpty())
        temporary.add(ScorePlayer("Temporary", 2))
        temporary.resetScores()
        assertEquals(listOf(ScorePlayer("Temporary", 0)), temporary.players.toList())
        memory.saveSettings(memory.settings().copy(rememberGameSessions = true))
        assertEquals(listOf(ScorePlayer("Saved", 8)), ScoreboardStore(context, "scoreboard_players", policy).players.toList())
    }

    @Test fun disabledChessReadsDefaultsAndPreservesPriorMatch() {
        val store = ChessMatchStore(context, policy)
        val saved = ChessMatch("Alex", "Sam", 1f, 0f, 1)
        store.save(saved)
        memory.saveSettings(memory.settings().copy(rememberGameSessions = false))
        assertEquals(ChessMatch("White", "Black", 0f, 0f, 0), store.load())
        store.save(ChessMatch("New", "Match", 0f, 0f, 0))
        memory.saveSettings(memory.settings().copy(rememberGameSessions = true))
        assertEquals(saved, store.load())
    }

    @Test fun explicitHistoryRestoreWhileDisabledChangesOnlyCurrentMatch() {
        val scoreboard = ScoreboardStore(context, "scoreboard_players", policy)
        scoreboard.add(ScorePlayer("Old", 1))
        val chess = ChessMatchStore(context, policy)
        val oldChess = ChessMatch("Old white", "Old black", 1f, 0f, 1)
        chess.save(oldChess)
        memory.saveSettings(memory.settings().copy(rememberGameSessions = false))
        assertTrue(scoreboard.restoreSnapshot(ScoreSnapshots.scoreboard(listOf(ScorePlayer("Imported", 7)))!!))
        assertEquals(listOf(ScorePlayer("Imported", 7)), scoreboard.players.toList())
        val importedChess = ChessMatch("New white", "New black", 0f, 1f, 1)
        assertEquals(importedChess, chess.restoreSnapshot(ScoreSnapshots.chess(importedChess)!!))
        memory.saveSettings(memory.settings().copy(rememberGameSessions = true))
        assertEquals(listOf(ScorePlayer("Old", 1)), ScoreboardStore(context, "scoreboard_players", policy).players.toList())
        assertEquals(oldChess, chess.load())
    }

    @Test fun disableWaitsForAnAlreadyStartedGameWriteAndBlocksLaterWrites() {
        raceAnInFlightWrite {
            memory.saveSettings(memory.settings().copy(rememberGameSessions = false))
        }
        val games = SimplePrefsStore(context, gameSession = true, sessionPolicy = policy)
        games.putInt("rps.wins", 99)
        assertEquals(7, prefs.getInt("rps.wins", 0))
        assertFalse(memory.settings().rememberGameSessions)
    }

    @Test fun clearRunsAfterAnAlreadyStartedGameWriteRatherThanBeingUndoneByIt() {
        raceAnInFlightWrite { policy.clearSavedSessions() }
        assertFalse(prefs.contains("rps.wins"))
    }

    private fun raceAnInFlightWrite(change: () -> Unit) {
        val entered = CountDownLatch(1)
        val releaseWriter = CountDownLatch(1)
        val mutationStarted = CountDownLatch(1)
        val mutationDone = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>(null)
        val games = SimplePrefsStore(context, gameSession = true, sessionPolicy = policy)
        val writer = thread {
            try {
                games.edit {
                    entered.countDown()
                    check(releaseWriter.await(5, TimeUnit.SECONDS))
                    putInt("rps.wins", 7)
                }
            } catch (error: Throwable) { failure.set(error) }
        }
        assertTrue(entered.await(5, TimeUnit.SECONDS))
        val mutation = thread {
            mutationStarted.countDown()
            try { change() } catch (error: Throwable) { failure.set(error) }
            finally { mutationDone.countDown() }
        }
        try {
            assertTrue(mutationStarted.await(5, TimeUnit.SECONDS))
            assertFalse(mutationDone.await(100, TimeUnit.MILLISECONDS))
        } finally {
            releaseWriter.countDown()
            writer.join(5000)
            mutation.join(5000)
        }
        assertFalse(writer.isAlive)
        assertFalse(mutation.isAlive)
        failure.get()?.let { throw AssertionError("Concurrent persistence failed", it) }
    }

    @Test fun explicitClearWorksWhileDisabledAndLeavesUnrelatedDataAlone() {
        val scoreboard = ScoreboardStore(context, "scoreboard_players", policy)
        scoreboard.add(ScorePlayer("Saved", 5))
        ChessMatchStore(context, policy).save(ChessMatch("Alex", "Sam", 1f, 0f, 1))
        prefs.edit().putString("team_picker.names", "Alex").putString("tambola.called", "1,2")
            .putInt("tambola.last_called", 2).putInt("rps.wins", 3)
            .putString("favorites", "tool/tip").putInt("counter.value", 7)
            .putString("palette", "#FFFFFF").commit()
        memory.saveSettings(memory.settings().copy(rememberGameSessions = false, historyEnabled = true))
        memory.record("tool/tip", emptyMap(), "20")
        assertTrue(policy.clearSavedSessions())
        assertFalse(prefs.contains("scoreboard_players_v2"))
        assertFalse(prefs.contains("chess_white_name"))
        assertFalse(prefs.contains("team_picker.names"))
        assertFalse(prefs.contains("tambola.called"))
        assertFalse(prefs.contains("rps.wins"))
        assertEquals(listOf(ScorePlayer("Saved", 5)), scoreboard.players.toList())
        assertEquals("tool/tip", prefs.getString("favorites", null))
        assertEquals(7, prefs.getInt("counter.value", 0))
        assertEquals("#FFFFFF", prefs.getString("palette", null))
        assertEquals(1, memory.history("tool/tip").size)
        memory.saveSettings(memory.settings().copy(rememberGameSessions = true))
        assertTrue(ScoreboardStore(context, "scoreboard_players", policy).players.isEmpty())
    }
}
