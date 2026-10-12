package com.dtolan.numbers.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.gameScoreDataStore by preferencesDataStore(name = "numbers_game_scores")

/** Whether a lower or higher value counts as a better score for a given game. */
enum class ScoreDirection { HIGHER_IS_BETTER, LOWER_IS_BETTER }

/**
 * Persists each mini-game's personal-best score, keyed by a game id + language — the Home hub
 * reads these to show "Best: N" badges on each tile, and each game shows/updates its own. A
 * plain Int is enough for every game so far (streak length, call count, sequence length); if a
 * future game needs a richer score shape, give it its own store rather than overloading this one.
 */
class GameScoreStore(private val context: Context) {

    private fun key(gameId: String, languageCode: String) = intPreferencesKey("score_${gameId}_$languageCode")

    fun bestScore(gameId: String, languageCode: String): Flow<Int?> =
        context.gameScoreDataStore.data.map { it[key(gameId, languageCode)] }

    suspend fun currentBestScore(gameId: String, languageCode: String): Int? =
        bestScore(gameId, languageCode).first()

    /** Stores [value] as the new best if it beats the existing one per [direction]. Returns true if it became the new best. */
    suspend fun submitScore(gameId: String, languageCode: String, value: Int, direction: ScoreDirection): Boolean {
        val existing = currentBestScore(gameId, languageCode)
        val isBetter = existing == null || when (direction) {
            ScoreDirection.HIGHER_IS_BETTER -> value > existing
            ScoreDirection.LOWER_IS_BETTER -> value < existing
        }
        if (isBetter) {
            context.gameScoreDataStore.edit { it[key(gameId, languageCode)] = value }
        }
        return isBetter
    }

    /** Levels unlocked across every Stories theme (each theme starts with 1), or null if none beyond the first. */
    fun storiesUnlockedTotal(languageCode: String): Flow<Int?> =
        combine(StoryTheme.entries.map { bestScore(storiesGameId(it), languageCode) }) { scores ->
            if (scores.all { it == null }) null else scores.sumOf { it ?: 1 }
        }

    companion object {
        const val GAME_BINGO = "bingo"
        const val GAME_SEQUENCE = "sequence"
        const val GAME_DICTATION = "dictation"
        const val GAME_SIMON = "simon"

        /** Stories mode keeps one unlock ladder per theme. */
        fun storiesGameId(theme: StoryTheme) = "stories_${theme.name.lowercase()}"
    }
}
