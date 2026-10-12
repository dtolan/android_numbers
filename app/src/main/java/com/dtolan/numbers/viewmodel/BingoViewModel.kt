package com.dtolan.numbers.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dtolan.numbers.data.GameScoreStore
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.Languages
import com.dtolan.numbers.data.ScoreDirection
import com.dtolan.numbers.logic.BingoBoard
import com.dtolan.numbers.tts.TtsController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class BingoUiState(
    val language: LanguagePack = Languages.FRENCH,
    val stageIndex: Int = 0,
    val board: List<Int> = emptyList(),
    val calledValues: Set<Int> = emptySet(),
    val marked: Set<Int> = emptySet(),
    val currentCalled: Int? = null,
    val callCount: Int = 0,
    val won: Boolean = false,
    val isNewBest: Boolean = false,
    val bestScore: Int? = null,
)

/** Number Bingo: a 3x3 card, numbers called one at a time, tap the ones you hear, complete a line to win. Score = calls-to-win (fewer is better). */
class BingoViewModel(app: Application) : AndroidViewModel(app) {
    private val tts = TtsController(app)
    private val scoreStore = GameScoreStore(app)

    private val _state = MutableStateFlow(BingoUiState())
    val state: StateFlow<BingoUiState> = _state.asStateFlow()

    private var callJob: Job? = null

    fun configure(language: LanguagePack, stageIndex: Int) {
        val languageChanged = _state.value.language.code != language.code
        _state.value = _state.value.copy(language = language, stageIndex = stageIndex)
        if (languageChanged) {
            viewModelScope.launch {
                val best = scoreStore.currentBestScore(GameScoreStore.GAME_BINGO, language.code)
                _state.value = _state.value.copy(bestScore = best)
            }
        }
    }

    fun newCard(speechRate: Float) {
        callJob?.cancel()
        val state = _state.value
        val stage = state.language.learnStages.getOrNull(state.stageIndex) ?: state.language.learnStages.firstOrNull() ?: return
        val pool = stage.values ?: (stage.min..stage.max).toList()
        val board = BingoBoard.generateBoard(pool)
        _state.value = state.copy(
            board = board,
            calledValues = emptySet(),
            marked = emptySet(),
            currentCalled = null,
            callCount = 0,
            won = false,
            isNewBest = false,
        )

        callJob = viewModelScope.launch {
            val remaining = pool.shuffled().toMutableList()
            while (isActive && remaining.isNotEmpty() && !_state.value.won) {
                val value = remaining.removeAt(0)
                val current = _state.value
                _state.value = current.copy(
                    currentCalled = value,
                    calledValues = current.calledValues + value,
                    callCount = current.callCount + 1,
                )
                tts.speakAndAwait(value.toString(), current.language.locale, speechRate)
                delay(CALL_GAP_MS)
            }
        }
    }

    fun markCell(value: Int) {
        val state = _state.value
        if (state.won || value !in state.calledValues || value in state.marked) return
        val newMarked = state.marked + value
        val won = BingoBoard.hasWon(state.board, newMarked)
        _state.value = state.copy(marked = newMarked, won = won)
        if (won) {
            callJob?.cancel()
            viewModelScope.launch {
                val isNewBest = scoreStore.submitScore(GameScoreStore.GAME_BINGO, state.language.code, state.callCount, ScoreDirection.LOWER_IS_BETTER)
                _state.value = _state.value.copy(isNewBest = isNewBest, bestScore = if (isNewBest) state.callCount else _state.value.bestScore)
            }
        }
    }

    override fun onCleared() {
        callJob?.cancel()
        tts.shutdown()
    }

    private companion object {
        const val CALL_GAP_MS = 2800L
    }
}
