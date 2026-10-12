package com.dtolan.numbers.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dtolan.numbers.data.GameScoreStore
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.Languages
import com.dtolan.numbers.data.LearnStage
import com.dtolan.numbers.data.ScoreDirection
import com.dtolan.numbers.logic.SequenceGenerator
import com.dtolan.numbers.logic.SequenceLevel
import com.dtolan.numbers.tts.TtsController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SequenceUiState(
    val language: LanguagePack = Languages.FRENCH,
    val stageIndex: Int = 0,
    val level: SequenceLevel = SequenceLevel.BASIC,
    val heardValues: List<Int> = emptyList(),
    val choices: List<String> = emptyList(),
    val correctNext: Int? = null,
    val selected: String? = null,
    val answered: Boolean = false,
    val isPlaying: Boolean = false,
    val streak: Int = 0,
    val bestStreak: Int? = null,
    val isNewBest: Boolean = false,
)

/**
 * "What comes next?" — hear a run of consecutive numbers, predict the next one. Only range-based
 * stages make sense here (a "Decades" stage's values aren't consecutive integers), so the caller
 * should offer [LanguagePack.learnStages] filtered to `values == null`.
 */
class SequenceViewModel(app: Application) : AndroidViewModel(app) {
    private val tts = TtsController(app)
    private val scoreStore = GameScoreStore(app)

    private val _state = MutableStateFlow(SequenceUiState())
    val state: StateFlow<SequenceUiState> = _state.asStateFlow()

    private var playbackJob: Job? = null

    fun configure(language: LanguagePack, stageIndex: Int) {
        val languageChanged = _state.value.language.code != language.code
        _state.value = _state.value.copy(language = language, stageIndex = stageIndex)
        if (languageChanged) {
            viewModelScope.launch {
                val best = scoreStore.currentBestScore(GameScoreStore.GAME_SEQUENCE, language.code)
                _state.value = _state.value.copy(bestStreak = best, streak = 0)
            }
        }
    }

    fun setLevel(level: SequenceLevel) {
        _state.value = _state.value.copy(level = level)
    }

    fun nextRound(rangeStages: List<LearnStage>, speechRate: Float) {
        playbackJob?.cancel()
        val state = _state.value
        val stage = rangeStages.getOrNull(state.stageIndex) ?: rangeStages.firstOrNull() ?: return
        val seq = SequenceGenerator.generate(stage.min, stage.max, level = state.level)
            ?: SequenceGenerator.generate(0, 100, level = state.level)
            ?: return

        _state.value = state.copy(
            heardValues = seq.values,
            choices = seq.choices,
            correctNext = seq.next,
            selected = null,
            answered = false,
            isPlaying = true,
        )
        playbackJob = viewModelScope.launch {
            for (value in seq.values) {
                tts.speakAndAwait(value.toString(), state.language.locale, speechRate)
                delay(NUMBER_GAP_MS)
            }
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    /** Replays the same run, slower than the initial listen — a second pass to catch what was missed, not a race against the same pace. */
    fun replay(speechRate: Float) {
        val state = _state.value
        if (state.isPlaying || state.heardValues.isEmpty()) return
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            _state.value = _state.value.copy(isPlaying = true)
            for (value in state.heardValues) {
                tts.speakAndAwait(value.toString(), state.language.locale, speechRate * REPLAY_RATE_FACTOR)
                delay(REPLAY_GAP_MS)
            }
            _state.value = _state.value.copy(isPlaying = false)
        }
    }

    fun chooseAnswer(choice: String, speechRate: Float) {
        val state = _state.value
        if (state.answered || state.isPlaying || state.correctNext == null) return
        val isCorrect = choice == state.correctNext.toString()
        val newStreak = if (isCorrect) state.streak + 1 else 0
        _state.value = state.copy(selected = choice, answered = true, streak = newStreak)
        if (isCorrect) {
            tts.speak(choice, state.language.locale, speechRate)
            viewModelScope.launch {
                val isNewBest = scoreStore.submitScore(GameScoreStore.GAME_SEQUENCE, state.language.code, newStreak, ScoreDirection.HIGHER_IS_BETTER)
                _state.value = _state.value.copy(isNewBest = isNewBest, bestStreak = if (isNewBest) newStreak else _state.value.bestStreak)
            }
        }
    }

    override fun onCleared() {
        playbackJob?.cancel()
        tts.shutdown()
    }

    private companion object {
        const val NUMBER_GAP_MS = 500L
        const val REPLAY_GAP_MS = 750L
        const val REPLAY_RATE_FACTOR = 0.75f
    }
}
