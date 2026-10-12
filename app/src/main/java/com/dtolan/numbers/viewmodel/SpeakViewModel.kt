package com.dtolan.numbers.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.Languages
import com.dtolan.numbers.logic.SpeakGenerator
import com.dtolan.numbers.logic.SpeakTask
import com.dtolan.numbers.logic.SpeechGrader
import com.dtolan.numbers.tts.TtsController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SpeakUiState(
    val language: LanguagePack = Languages.FRENCH,
    val level: Int = 0,
    val task: SpeakTask? = null,
    val isListening: Boolean = false,
    /** What the recognizer heard (its best guess, or the guess that matched). */
    val heard: String? = null,
    /** A recognizer problem to show instead of grading (nothing heard, error code…). */
    val message: String? = null,
    val answered: Boolean = false,
    val isCorrect: Boolean = false,
    /** Correct answers in a row on this level — just encouragement, nothing is locked. */
    val streak: Int = 0,
    /** Saying an already-answered task again for practice — doesn't move the streak either way. */
    val isRepeat: Boolean = false,
)

/**
 * Speak mode: the learner answers out loud. The screen owns the Android SpeechRecognizer (it needs
 * the Activity's context and the mic permission) and hands its guesses to [onHeard], which grades
 * them with [SpeechGrader]. Every level is open — the learner picks whichever they want to try.
 */
class SpeakViewModel(app: Application) : AndroidViewModel(app) {
    private val tts = TtsController(app)

    private val _state = MutableStateFlow(SpeakUiState())
    val state: StateFlow<SpeakUiState> = _state.asStateFlow()

    fun start(language: LanguagePack, speechRate: Float) {
        _state.value = _state.value.copy(language = language, streak = 0)
        next(speechRate)
    }

    fun setLevel(level: Int, speechRate: Float) {
        if (level !in 0 until SpeakGenerator.LEVEL_COUNT) return
        _state.value = _state.value.copy(level = level, streak = 0)
        next(speechRate)
    }

    fun next(speechRate: Float) {
        val state = _state.value
        val task = SpeakGenerator.generate(state.level, state.language)
        _state.value = state.copy(task = task, heard = null, message = null, answered = false, isCorrect = false, isRepeat = false)
        replay(speechRate)
    }

    /** Re-reads a heard task (next/previous, maths, positions); visual tasks have nothing to say. */
    fun replay(speechRate: Float) {
        _state.value.task?.speakText?.let { tts.speak(it, _state.value.language.locale, speechRate) }
    }

    /** Says the correct answer, as a model to copy. */
    fun sayAnswer(speechRate: Float) {
        val state = _state.value
        state.task?.let { tts.speak(it.answerSpeech(state.language), state.language.locale, speechRate) }
    }

    /** Clears the last attempt so the same task can be said again — after a miss, or just to repeat it. */
    fun retry() {
        _state.value = _state.value.copy(heard = null, message = null, answered = false, isRepeat = true)
    }

    fun onListeningStarted() {
        tts.stop()
        _state.value = _state.value.copy(isListening = true, message = null)
    }

    fun onListeningStopped() {
        _state.value = _state.value.copy(isListening = false)
    }

    /** Recognizer trouble (no speech, timeout, error) — shown, not graded. */
    fun onRecognitionProblem(message: String) {
        _state.value = _state.value.copy(isListening = false, message = message)
    }

    fun onHeard(guesses: List<String>) {
        val state = _state.value
        val task = state.task ?: return
        if (state.answered || guesses.isEmpty()) return
        val match = guesses.firstOrNull { SpeechGrader.matches(task, listOf(it), state.language) }
        val isCorrect = match != null
        _state.value = state.copy(
            isListening = false,
            heard = match ?: guesses.first(),
            message = null,
            answered = true,
            isCorrect = isCorrect,
            streak = when {
                state.isRepeat -> state.streak
                isCorrect -> state.streak + 1
                else -> 0
            },
        )
    }

    override fun onCleared() {
        tts.shutdown()
    }
}
