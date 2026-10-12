package com.dtolan.numbers.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dtolan.numbers.data.Category
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.Languages
import com.dtolan.numbers.data.SettingsStore
import com.dtolan.numbers.logic.AdaptiveTiming
import com.dtolan.numbers.logic.PracticeSettings
import com.dtolan.numbers.logic.Question
import com.dtolan.numbers.logic.QuestionGenerators
import com.dtolan.numbers.tts.TtsController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class FeedbackState {
    data object None : FeedbackState()
    data class Correct(val displayAnswer: String) : FeedbackState()
    data class Incorrect(val displayAnswer: String) : FeedbackState()
    data class StageComplete(val message: String) : FeedbackState()
    /** Correct, but the answer arrived after the stage's current adaptive time limit. */
    data class TooSlow(val displayAnswer: String, val elapsedMs: Long, val limitMs: Long) : FeedbackState()
}

/** The Home hub, or one of the practice modes/games reachable from it. */
enum class AppMode { HOME, PRACTICE, LEARN, LISTEN, BINGO, SEQUENCE, DICTATION, SIMON, STORIES, COUNT, SPEAK }

data class PracticeUiState(
    val language: LanguagePack = Languages.FRENCH,
    val settings: PracticeSettings = PracticeSettings(),
    val currentQuestion: Question? = null,
    val answerText: String = "",
    val feedback: FeedbackState = FeedbackState.None,
    val showAnswerForm: Boolean = true,
    val correct: Int = 0,
    val total: Int = 0,
    val streak: Int = 0,
    val voiceAvailable: Boolean = true,
    val loaded: Boolean = false,
    val mode: AppMode = AppMode.HOME,
    val learnStageIndex: Int = 0,
    val learnUnlockedIndex: Int = 0,
    val learnStreak: Int = 0,
    val learnJustCompletedStage: Boolean = false,
    /** The current adaptive response-time limit (ms) an answer must beat to count toward the streak. */
    val learnTimeLimitMs: Long = 0,
    /** True while the choices are shown but not yet answerable — the "find the numbers" preview beat before the word is spoken and the clock starts. */
    val learnPreviewActive: Boolean = false,
    /** Global Listen-mode "confirm in" language preference — the listener's own language, independent of whichever language is being drilled. Null = off. */
    val confirmLanguageCode: String? = null,
)

class PracticeViewModel(app: Application) : AndroidViewModel(app) {
    private val store = SettingsStore(app)
    private val tts = TtsController(app)

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    /** elapsedRealtime() when the current question became answerable; not part of [PracticeUiState] since the UI doesn't need it. */
    private var learnQuestionStartedAt: Long = 0L

    /** Bumped on every new Learn question so a stale delayed "reveal" from a since-abandoned question is a no-op. */
    private var learnQuestionGeneration = 0

    init {
        tts.onEngineReady = { refreshVoiceAvailability() }
        viewModelScope.launch {
            val code = store.currentLanguageCode()
            val settings = store.currentSettings()
            val lang = Languages.byCode(code)
            val unlocked = store.currentLearnUnlockedIndex(code)
            val stageIndex = clampStageIndex(unlocked, lang)
            val confirmLanguageCode = store.currentConfirmLanguageCode()
            _uiState.update {
                it.copy(
                    language = lang,
                    settings = settings,
                    loaded = true,
                    voiceAvailable = tts.hasUsableVoice(lang.locale),
                    learnUnlockedIndex = unlocked,
                    learnStageIndex = stageIndex,
                    learnTimeLimitMs = lang.learnStages.getOrNull(stageIndex)?.startTimeLimitMs ?: 0,
                    confirmLanguageCode = confirmLanguageCode,
                )
            }
            nextQuestion()
        }
    }

    private fun clampStageIndex(index: Int, lang: LanguagePack): Int =
        index.coerceIn(0, (lang.learnStages.size - 1).coerceAtLeast(0))

    private fun refreshVoiceAvailability() {
        _uiState.update { it.copy(voiceAvailable = tts.hasUsableVoice(it.language.locale)) }
    }

    fun setLanguage(code: String) {
        val lang = Languages.byCode(code)
        _uiState.update { it.copy(language = lang, voiceAvailable = tts.hasUsableVoice(lang.locale)) }
        viewModelScope.launch {
            store.setLanguage(code)
            val unlocked = store.currentLearnUnlockedIndex(code)
            val stageIndex = clampStageIndex(unlocked, lang)
            _uiState.update {
                it.copy(
                    learnUnlockedIndex = unlocked,
                    learnStageIndex = stageIndex,
                    learnStreak = 0,
                    learnJustCompletedStage = false,
                    learnTimeLimitMs = lang.learnStages.getOrNull(stageIndex)?.startTimeLimitMs ?: 0,
                )
            }
            nextQuestion()
        }
    }

    fun setMode(mode: AppMode) {
        if (_uiState.value.mode == mode) return
        _uiState.update { it.copy(mode = mode, feedback = FeedbackState.None) }
        nextQuestion()
    }

    /** Jumps to an already-unlocked Learn stage (for review) or the next one just unlocked. */
    fun selectLearnStage(index: Int) {
        val state = _uiState.value
        if (index < 0 || index > state.learnUnlockedIndex) return
        val limit = state.language.learnStages.getOrNull(index)?.startTimeLimitMs ?: 0
        _uiState.update { it.copy(learnStageIndex = index, learnStreak = 0, learnJustCompletedStage = false, learnTimeLimitMs = limit) }
        nextQuestion()
    }

    /** Flash-drill tap-to-answer entry point for Learn mode. */
    fun chooseAnswer(choice: String) {
        _uiState.update { it.copy(answerText = choice) }
        submitAnswer()
    }

    fun toggleCategory(category: Category) {
        val current = _uiState.value.settings.enabledCategories
        val updated = if (category in current) current - category else current + category
        _uiState.update { it.copy(settings = it.settings.copy(enabledCategories = updated)) }
        viewModelScope.launch { store.setCategoryEnabled(category, category in updated) }
        nextQuestion()
    }

    fun updateRanges(minVal: Int, maxVal: Int, decimalPlaces: Int, yearMin: Int, yearMax: Int) {
        _uiState.update {
            it.copy(settings = it.settings.copy(minVal = minVal, maxVal = maxVal, decimalPlaces = decimalPlaces, yearMin = yearMin, yearMax = yearMax))
        }
        viewModelScope.launch { store.updateRanges(minVal, maxVal, decimalPlaces, yearMin, yearMax) }
    }

    fun setSpeechRate(rate: Float) {
        _uiState.update { it.copy(settings = it.settings.copy(speechRate = rate)) }
        viewModelScope.launch { store.setSpeechRate(rate) }
    }

    /** Sets the global Listen-mode "confirm in" language (null = off). */
    fun setConfirmLanguage(code: String?) {
        _uiState.update { it.copy(confirmLanguageCode = code) }
        viewModelScope.launch { store.setConfirmLanguageCode(code) }
    }

    fun setAnswerText(text: String) {
        _uiState.update { it.copy(answerText = text) }
    }

    fun nextQuestion() {
        val state = _uiState.value
        when (state.mode) {
            AppMode.PRACTICE -> nextPracticeQuestion(state)
            AppMode.LEARN -> nextLearnQuestion(state)
            // These modes aren't driven by this ViewModel: Listen runs in ListeningService, and
            // each mini-game (Bingo/Sequence/Dictation/Simon) owns its own ViewModel and state.
            AppMode.HOME, AppMode.LISTEN, AppMode.BINGO, AppMode.SEQUENCE, AppMode.DICTATION, AppMode.SIMON, AppMode.STORIES, AppMode.COUNT, AppMode.SPEAK -> {}
        }
    }

    private fun nextPracticeQuestion(state: PracticeUiState) {
        val cats = state.settings.enabledCategories
        if (cats.isEmpty()) {
            _uiState.update { it.copy(currentQuestion = null, feedback = FeedbackState.None, showAnswerForm = false, answerText = "") }
            return
        }
        val category = cats.toList().random()
        val question = QuestionGenerators.generate(category, state.language, state.settings)
        _uiState.update {
            it.copy(currentQuestion = question, answerText = "", feedback = FeedbackState.None, showAnswerForm = true, learnJustCompletedStage = false)
        }
        speakCurrent()
    }

    private fun nextLearnQuestion(state: PracticeUiState) {
        val stages = state.language.learnStages
        if (stages.isEmpty()) return
        // Advancing off a just-mastered stage: move into the (now unlocked) next one, back at its
        // generous starting time limit — speed pressure ramps up again from scratch each stage.
        val advancing = state.learnJustCompletedStage && state.learnStageIndex < stages.size - 1
        val stageIndex = if (advancing) state.learnStageIndex + 1 else state.learnStageIndex
        val streak = if (advancing) 0 else state.learnStreak
        val stage = stages[stageIndex.coerceIn(0, stages.size - 1)]
        val timeLimit = if (advancing) stage.startTimeLimitMs else state.learnTimeLimitMs
        val question = QuestionGenerators.genLearnQuestion(state.language, stage)

        // Show the choices first, silently, so the player can see where each number is before
        // having to react to anything — then speak the word and start the clock a beat later.
        // Without this gap, players end up hunting for the right button instead of recalling the
        // number, since the buttons shuffle position every question.
        val generation = ++learnQuestionGeneration
        _uiState.update {
            it.copy(
                currentQuestion = question,
                answerText = "",
                feedback = FeedbackState.None,
                showAnswerForm = true,
                learnJustCompletedStage = false,
                learnStageIndex = stageIndex,
                learnStreak = streak,
                learnTimeLimitMs = timeLimit,
                learnPreviewActive = true,
            )
        }
        viewModelScope.launch {
            delay(LEARN_PREVIEW_DELAY_MS)
            if (learnQuestionGeneration != generation) return@launch // superseded by a newer question
            learnQuestionStartedAt = SystemClock.elapsedRealtime()
            _uiState.update { it.copy(learnPreviewActive = false) }
            speakCurrent()
        }
    }

    fun replay() = speakCurrent()

    private fun speakCurrent() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        tts.speak(question.speakText, state.language.locale, state.settings.speechRate)
    }

    fun submitAnswer() {
        val state = _uiState.value
        val question = state.currentQuestion ?: return
        if (state.mode == AppMode.LEARN && state.learnPreviewActive) return
        val input = state.answerText.trim()
        if (input.isEmpty()) return
        val isCorrect = question.checkAnswer(input)
        when (state.mode) {
            AppMode.PRACTICE -> submitPracticeAnswer(question, isCorrect)
            AppMode.LEARN -> submitLearnAnswer(state, question, isCorrect)
            AppMode.HOME, AppMode.LISTEN, AppMode.BINGO, AppMode.SEQUENCE, AppMode.DICTATION, AppMode.SIMON, AppMode.STORIES, AppMode.COUNT, AppMode.SPEAK -> {}
        }
    }

    private fun submitPracticeAnswer(question: Question, isCorrect: Boolean) {
        _uiState.update {
            it.copy(
                correct = it.correct + if (isCorrect) 1 else 0,
                total = it.total + 1,
                streak = if (isCorrect) it.streak + 1 else 0,
                feedback = if (isCorrect) FeedbackState.Correct(question.displayAnswer) else FeedbackState.Incorrect(question.displayAnswer),
                showAnswerForm = false,
            )
        }
    }

    private fun submitLearnAnswer(state: PracticeUiState, question: Question, isCorrect: Boolean) {
        val stages = state.language.learnStages
        val stage = stages[state.learnStageIndex.coerceIn(0, stages.size - 1)]
        val elapsedMs = SystemClock.elapsedRealtime() - learnQuestionStartedAt
        val withinTime = elapsedMs <= state.learnTimeLimitMs
        // Only a correct AND fast-enough answer counts toward the mastery streak — a correct but
        // slow answer means the answer is still being computed rather than recalled, so it resets
        // the streak (and the time pressure) exactly like a wrong answer would.
        val qualifies = isCorrect && withinTime
        val newStreak = if (qualifies) state.learnStreak + 1 else 0
        val newTimeLimit = if (qualifies) {
            AdaptiveTiming.shrink(state.learnTimeLimitMs, stage.minTimeLimitMs, stage.timeDecayFactor)
        } else {
            stage.startTimeLimitMs
        }
        // Only the crossing answer (streak hits the threshold for the first time) counts as
        // "just completed" — otherwise every correct answer after mastery would re-fire it.
        val justCompleted = qualifies && state.learnStreak < stage.requiredStreak && newStreak >= stage.requiredStreak &&
            state.learnStageIndex == state.learnUnlockedIndex
        var newUnlocked = state.learnUnlockedIndex
        if (justCompleted && state.learnStageIndex < stages.size - 1) {
            newUnlocked = state.learnStageIndex + 1
            viewModelScope.launch { store.setLearnUnlockedIndex(state.language.code, newUnlocked) }
        }
        val feedback = when {
            justCompleted && state.learnStageIndex == stages.size - 1 -> FeedbackState.StageComplete(state.language.ui.learnAllStagesComplete)
            justCompleted -> FeedbackState.StageComplete(String.format(state.language.ui.learnStageComplete, stages[state.learnStageIndex + 1].title))
            isCorrect && withinTime -> FeedbackState.Correct(question.displayAnswer)
            isCorrect -> FeedbackState.TooSlow(question.displayAnswer, elapsedMs, state.learnTimeLimitMs)
            else -> FeedbackState.Incorrect(question.displayAnswer)
        }
        _uiState.update {
            it.copy(
                learnStreak = newStreak,
                learnUnlockedIndex = newUnlocked,
                learnTimeLimitMs = newTimeLimit,
                feedback = feedback,
                showAnswerForm = false,
                learnJustCompletedStage = justCompleted,
            )
        }
    }

    fun resetScore() {
        _uiState.update { it.copy(correct = 0, total = 0, streak = 0) }
    }

    override fun onCleared() {
        tts.shutdown()
    }

    private companion object {
        const val LEARN_PREVIEW_DELAY_MS = 2000L
    }
}
