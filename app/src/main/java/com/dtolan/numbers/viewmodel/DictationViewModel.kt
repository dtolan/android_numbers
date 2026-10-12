package com.dtolan.numbers.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dtolan.numbers.data.Category
import com.dtolan.numbers.data.GameScoreStore
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.Languages
import com.dtolan.numbers.data.ScoreDirection
import com.dtolan.numbers.logic.PracticeSettings
import com.dtolan.numbers.logic.Question
import com.dtolan.numbers.logic.QuestionGenerators
import com.dtolan.numbers.tts.TtsController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DictationUiState(
    val language: LanguagePack = Languages.FRENCH,
    val category: Category = Category.PHONE,
    val question: Question? = null,
    val answerText: String = "",
    val answered: Boolean = false,
    val isCorrect: Boolean = false,
    val streak: Int = 0,
    val bestStreak: Int? = null,
    val isNewBest: Boolean = false,
    val replaysLeft: Int = MAX_REPLAYS,
) {
    companion object {
        const val MAX_REPLAYS = 2
    }
}

/** Hear a full phone number or date at natural speed, transcribe the whole thing — limited replays, like real dictation. */
class DictationViewModel(app: Application) : AndroidViewModel(app) {
    private val tts = TtsController(app)
    private val scoreStore = GameScoreStore(app)
    private val settings = PracticeSettings()

    private val _state = MutableStateFlow(DictationUiState())
    val state: StateFlow<DictationUiState> = _state.asStateFlow()

    fun configure(language: LanguagePack) {
        val languageChanged = _state.value.language.code != language.code
        _state.value = _state.value.copy(language = language)
        if (languageChanged) {
            viewModelScope.launch {
                val best = scoreStore.currentBestScore(GameScoreStore.GAME_DICTATION, language.code)
                _state.value = _state.value.copy(bestStreak = best, streak = 0)
            }
        }
    }

    fun setCategory(category: Category, speechRate: Float) {
        _state.value = _state.value.copy(category = category)
        nextQuestion(speechRate)
    }

    fun nextQuestion(speechRate: Float) {
        val state = _state.value
        val question = QuestionGenerators.generate(state.category, state.language, settings)
        _state.value = state.copy(question = question, answerText = "", answered = false, replaysLeft = DictationUiState.MAX_REPLAYS)
        tts.speak(question.speakText, state.language.locale, speechRate)
    }

    fun replay(speechRate: Float) {
        val state = _state.value
        val question = state.question ?: return
        if (state.replaysLeft <= 0) return
        _state.value = state.copy(replaysLeft = state.replaysLeft - 1)
        tts.speak(question.speakText, state.language.locale, speechRate)
    }

    fun setAnswerText(text: String) {
        _state.value = _state.value.copy(answerText = text)
    }

    fun submit() {
        val state = _state.value
        val question = state.question ?: return
        val input = state.answerText.trim()
        if (input.isEmpty() || state.answered) return
        val isCorrect = question.checkAnswer(input)
        val newStreak = if (isCorrect) state.streak + 1 else 0
        _state.value = state.copy(answered = true, isCorrect = isCorrect, streak = newStreak)
        if (isCorrect) {
            viewModelScope.launch {
                val isNewBest = scoreStore.submitScore(GameScoreStore.GAME_DICTATION, state.language.code, newStreak, ScoreDirection.HIGHER_IS_BETTER)
                _state.value = _state.value.copy(isNewBest = isNewBest, bestStreak = if (isNewBest) newStreak else _state.value.bestStreak)
            }
        }
    }

    override fun onCleared() {
        tts.shutdown()
    }
}
