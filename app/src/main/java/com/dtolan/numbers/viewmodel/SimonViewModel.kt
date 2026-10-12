package com.dtolan.numbers.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dtolan.numbers.data.GameScoreStore
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.Languages
import com.dtolan.numbers.data.ScoreDirection
import com.dtolan.numbers.tts.TtsController
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SimonUiState(
    val language: LanguagePack = Languages.FRENCH,
    val stageIndex: Int = 0,
    val keypad: List<Int> = emptyList(),
    val sequence: List<Int> = emptyList(),
    val playerProgress: Int = 0,
    val isPlayingBack: Boolean = true,
    val isGameOver: Boolean = false,
    val started: Boolean = false,
    val bestLength: Int? = null,
    val isNewBest: Boolean = false,
    val lastWrong: Int? = null,
)

/** Growing spoken sequence from a fixed keypad — tap it back in order; one mistake ends it. Score = longest sequence reached. */
class SimonViewModel(app: Application) : AndroidViewModel(app) {
    private val tts = TtsController(app)
    private val scoreStore = GameScoreStore(app)

    private val _state = MutableStateFlow(SimonUiState())
    val state: StateFlow<SimonUiState> = _state.asStateFlow()

    private var playbackJob: Job? = null

    fun configure(language: LanguagePack, stageIndex: Int) {
        val languageChanged = _state.value.language.code != language.code
        _state.value = _state.value.copy(language = language, stageIndex = stageIndex)
        if (languageChanged) {
            viewModelScope.launch {
                val best = scoreStore.currentBestScore(GameScoreStore.GAME_SIMON, language.code)
                _state.value = _state.value.copy(bestLength = best)
            }
        }
    }

    fun start(speechRate: Float) {
        playbackJob?.cancel()
        val state = _state.value
        val stage = state.language.learnStages.getOrNull(state.stageIndex) ?: state.language.learnStages.firstOrNull() ?: return
        val pool = (stage.values ?: (stage.min..stage.max).toList()).distinct()
        val keypad = pool.shuffled().take(KEYPAD_SIZE.coerceAtMost(pool.size))
        if (keypad.isEmpty()) return
        val firstValue = keypad[Random.nextInt(keypad.size)]
        _state.value = state.copy(
            keypad = keypad,
            sequence = listOf(firstValue),
            playerProgress = 0,
            isGameOver = false,
            started = true,
            isNewBest = false,
            lastWrong = null,
        )
        playback(speechRate)
    }

    private fun playback(speechRate: Float) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            _state.value = _state.value.copy(isPlayingBack = true, playerProgress = 0, lastWrong = null)
            val state = _state.value
            for (value in state.sequence) {
                tts.speakAndAwait(value.toString(), state.language.locale, speechRate)
                delay(SEQUENCE_GAP_MS)
            }
            _state.value = _state.value.copy(isPlayingBack = false)
        }
    }

    fun replaySequence(speechRate: Float) {
        if (_state.value.isPlayingBack || _state.value.isGameOver) return
        playback(speechRate)
    }

    fun tapKeypad(value: Int, speechRate: Float) {
        val state = _state.value
        if (state.isPlayingBack || state.isGameOver || !state.started) return
        val expected = state.sequence.getOrNull(state.playerProgress)
        if (value != expected) {
            val finalLength = state.sequence.size - 1
            _state.value = state.copy(isGameOver = true, lastWrong = value)
            viewModelScope.launch {
                val isNewBest = scoreStore.submitScore(GameScoreStore.GAME_SIMON, state.language.code, finalLength, ScoreDirection.HIGHER_IS_BETTER)
                _state.value = _state.value.copy(isNewBest = isNewBest, bestLength = if (isNewBest) finalLength else _state.value.bestLength)
            }
            return
        }
        val newProgress = state.playerProgress + 1
        if (newProgress == state.sequence.size) {
            // Round complete — grow the sequence and play it back again.
            val nextValue = state.keypad[Random.nextInt(state.keypad.size)]
            _state.value = state.copy(sequence = state.sequence + nextValue, playerProgress = newProgress)
            viewModelScope.launch {
                delay(ROUND_COMPLETE_PAUSE_MS)
                playback(speechRate)
            }
        } else {
            _state.value = state.copy(playerProgress = newProgress)
        }
    }

    override fun onCleared() {
        playbackJob?.cancel()
        tts.shutdown()
    }

    private companion object {
        const val KEYPAD_SIZE = 6
        const val SEQUENCE_GAP_MS = 550L
        const val ROUND_COMPLETE_PAUSE_MS = 700L
    }
}
