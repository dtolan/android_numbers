package com.dtolan.numbers.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dtolan.numbers.data.GameScoreStore
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.Languages
import com.dtolan.numbers.data.ScoreDirection
import com.dtolan.numbers.data.StoryTheme
import com.dtolan.numbers.logic.PictureCue
import com.dtolan.numbers.logic.Speaker
import com.dtolan.numbers.logic.Story
import com.dtolan.numbers.logic.StoryGenerator
import com.dtolan.numbers.logic.pictureCues
import com.dtolan.numbers.tts.TtsController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StoryUiState(
    val language: LanguagePack = Languages.FRENCH,
    val theme: StoryTheme = StoryTheme.SHOPPING,
    val level: Int = 0,
    /** Highest unlocked level index per theme; levels above it are locked. */
    val unlocked: Map<StoryTheme, Int> = emptyMap(),
    val story: Story? = null,
    val answerText: String = "",
    val answered: Boolean = false,
    val isCorrect: Boolean = false,
    val levelStreak: Int = 0,
    /** Set when the last answer unlocked the next level; the learner moves up manually. */
    val justUnlocked: Boolean = false,
    /** The counted item the voice is saying right now ("3 pommes" → 🍎🍎🍎), shown as pictures; null between items. */
    val cue: PictureCue? = null,
) {
    val unlockedLevel: Int get() = unlocked[theme] ?: 0
    val levelCount: Int get() = StoryGenerator.levelCount(theme)

    /** True when playing the highest unlocked level and there is still a level above it to earn. */
    val isGateLevel: Boolean get() = level == unlockedLevel && level < levelCount - 1

    companion object {
        const val STREAK_TO_UNLOCK = 5
        const val CUE_LINGER_MS = 1500L
    }
}

/**
 * Spoken word problems grouped into themes, each with its own gated ladder of levels:
 * [StoryUiState.STREAK_TO_UNLOCK] correct answers in a row on a theme's highest unlocked level
 * unlock the next one, which the learner then chooses to move to. Unlock progress persists per
 * theme and language via [GameScoreStore] (score = number of unlocked levels).
 */
class StoryViewModel(app: Application) : AndroidViewModel(app) {
    private val tts = TtsController(app)
    private val scoreStore = GameScoreStore(app)

    private val _state = MutableStateFlow(StoryUiState())
    val state: StateFlow<StoryUiState> = _state.asStateFlow()

    /** Loads [language]'s unlock progress for every theme, then speaks the first story. */
    fun start(language: LanguagePack, speechRate: Float) {
        viewModelScope.launch {
            val unlocked = StoryTheme.entries.associateWith { theme ->
                val stored = scoreStore.currentBestScore(GameScoreStore.storiesGameId(theme), language.code) ?: 1
                (stored - 1).coerceIn(0, StoryGenerator.levelCount(theme) - 1)
            }
            val current = _state.value
            val sameLanguage = current.language.code == language.code
            val level = if (sameLanguage) current.level.coerceAtMost(unlocked.getValue(current.theme)) else unlocked.getValue(current.theme)
            _state.value = current.copy(language = language, unlocked = unlocked, level = level, levelStreak = 0, justUnlocked = false)
            nextQuestion(speechRate)
        }
    }

    /** Switches theme, jumping to that theme's highest unlocked level. */
    fun setTheme(theme: StoryTheme, speechRate: Float) {
        val state = _state.value
        if (state.theme == theme) return
        _state.value = state.copy(theme = theme, level = state.unlocked[theme] ?: 0, levelStreak = 0, justUnlocked = false)
        nextQuestion(speechRate)
    }

    fun setLevel(level: Int, speechRate: Float) {
        val state = _state.value
        if (level !in 0..state.unlockedLevel) return
        _state.value = state.copy(level = level, levelStreak = 0, justUnlocked = false)
        nextQuestion(speechRate)
    }

    fun nextQuestion(speechRate: Float) {
        val state = _state.value
        val story = StoryGenerator.generate(state.theme, state.level, state.language)
        _state.value = state.copy(story = story, answerText = "", answered = false, justUnlocked = false)
        speak(story, speechRate)
    }

    fun replay(speechRate: Float) {
        _state.value.story?.let { speak(it, speechRate) }
    }

    /** Bumped per playback so word callbacks from an older, interrupted story are ignored. */
    private var playback = 0

    private fun speak(story: Story, speechRate: Float) {
        val lines = story.lines.map { line ->
            line.text to when (line.speaker) {
                Speaker.NARRATOR -> 1f
                Speaker.YOU -> 1.2f
                Speaker.OTHER -> 0.8f
            }
        }
        val cues = pictureCues(story, _state.value.language.stories)
        val token = ++playback
        var wordTimingSeen = false
        _state.update { it.copy(cue = null) }
        tts.speakLines(lines, _state.value.language.locale, speechRate) { line, offset ->
            if (token != playback) return@speakLines
            when {
                // Finished: leave the last picture up a moment, then clear it.
                line >= lines.size -> viewModelScope.launch {
                    delay(StoryUiState.CUE_LINGER_MS)
                    if (token == playback) _state.update { it.copy(cue = null) }
                }
                // Word-level timing: show the counted item whose number the voice has just reached.
                offset > 0 -> {
                    wordTimingSeen = true
                    cues.lastOrNull { it.lineIndex == line && it.start <= offset }
                        ?.let { cue -> _state.update { it.copy(cue = cue) } }
                }
                // A line starting with no word timing so far (engine without range callbacks, or a
                // noun at the very start of the line): fall back to the line's first item.
                else -> cues.firstOrNull { it.lineIndex == line && (!wordTimingSeen && line > 0 || it.start == 0) }
                    ?.let { cue -> _state.update { it.copy(cue = cue) } }
            }
        }
    }

    fun setAnswerText(text: String) {
        _state.value = _state.value.copy(answerText = text)
    }

    fun submit() {
        val state = _state.value
        val question = state.story?.question ?: return
        val input = state.answerText.trim()
        if (input.isEmpty() || state.answered) return
        val isCorrect = question.checkAnswer(input)
        val streak = if (isCorrect) state.levelStreak + 1 else 0
        val unlocks = state.isGateLevel && streak >= StoryUiState.STREAK_TO_UNLOCK
        val unlocked = if (unlocks) state.unlocked + (state.theme to state.level + 1) else state.unlocked
        _state.value = state.copy(
            answered = true,
            isCorrect = isCorrect,
            levelStreak = streak,
            unlocked = unlocked,
            justUnlocked = unlocks,
        )
        if (unlocks) {
            viewModelScope.launch {
                scoreStore.submitScore(
                    GameScoreStore.storiesGameId(state.theme),
                    state.language.code,
                    state.level + 2,
                    ScoreDirection.HIGHER_IS_BETTER,
                )
            }
        }
    }

    override fun onCleared() {
        tts.shutdown()
    }
}
