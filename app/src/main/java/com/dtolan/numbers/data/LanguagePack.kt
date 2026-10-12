package com.dtolan.numbers.data

import java.util.Locale
import kotlin.random.Random

/** Whether a language writes dates day-first or month-first. */
enum class DateOrder { DMY, MDY }

/** All the on-screen text for one language's UI chrome (not the quiz content itself). */
data class UiStrings(
    val subtitle: String,
    val settingsTitle: String,
    val categoriesLabel: String,
    val minLabel: String,
    val maxLabel: String,
    val decimalsLabel: String,
    val yearMinLabel: String,
    val yearMaxLabel: String,
    val languageLabel: String,
    val voiceLabel: String,
    val rateLabel: String,
    val listenButton: String,
    val replayButton: String,
    val validateButton: String,
    val nextButton: String,
    val resetButton: String,
    val correctLabel: String,
    val totalLabel: String,
    val streakLabel: String,
    val chooseCategoryWarning: String,
    val feedbackCorrect: String,   // format with 1 %s = displayAnswer
    val feedbackIncorrect: String, // format with 1 %s = displayAnswer
    val voiceMissingTitle: String,
    val voiceMissingBody: String,  // format with 1 %s = language display name
    val installVoiceButton: String,
    val practiceModeLabel: String,
    val practiceModeSubtitle: String,
    val learnModeLabel: String,
    val learnModeSubtitle: String,
    val learnStageProgress: String,     // format with %d streak, %d required streak, %.1f time limit (s)
    val learnStageComplete: String,     // format with 1 %s = next stage's title
    val learnAllStagesComplete: String,
    val learnNextStageButton: String,
    val learnTooSlowFeedback: String,   // format with %s displayAnswer, %.1f elapsed (s), %.1f limit (s)
    val learnGetReadyLabel: String,
    val listenModeLabel: String,
    val listenModeSubtitle: String,
    val listenHint: String,
    val listenStartButton: String,
    val listenPauseButton: String,
    val listenResumeButton: String,
    val listenStopButton: String,
    val listenConfirmLabel: String,
    val listenConfirmOff: String,
    val listenRunningStatus: String,
    val listenPausedStatus: String,
    val listenModePickerLabel: String,
    val listenModeSpacedLabel: String,
    val listenModeSequentialLabel: String,
    val listenModeShuffleLabel: String,
    val listenModeAlwaysConfirmLabel: String,
    val homeTitle: String,
    val bestScoreLabel: String,          // format with 1 %d = best score
    val bingoModeLabel: String,
    val bingoModeSubtitle: String,
    val bingoHint: String,
    val bingoNewCardButton: String,
    val bingoCallsLabel: String,         // format with 1 %d = number of calls so far
    val bingoWinMessage: String,         // format with 1 %d = calls it took to win
    val sequenceModeLabel: String,
    val sequenceModeSubtitle: String,
    val sequenceHint: String,
    val sequenceProgressLabel: String,   // format with 2 %d = current streak, best streak
    val sequenceLevelBasicLabel: String,
    val sequenceLevelAdvancedLabel: String,
    val dictationModeLabel: String,
    val dictationModeSubtitle: String,
    val dictationHint: String,
    val dictationProgressLabel: String,  // format with 2 %d = current streak, best streak
    val dictationReplaysLabel: String,   // format with 1 %d = replays left
    val simonModeLabel: String,
    val simonModeSubtitle: String,
    val simonHint: String,
    val simonStartButton: String,
    val simonRoundLabel: String,         // format with 1 %d = current sequence length
    val simonGameOverMessage: String,    // format with 1 %d = sequence length reached
    val storyModeLabel: String,
    val storyModeSubtitle: String,
    val storyHint: String,
    val storyLevelLabel: String,         // format with %d level number, %s level title
    val storyProgressLabel: String,      // format with %d streak, %d streak needed to unlock
    val storyUnlockedMessage: String,    // format with 1 %s = newly unlocked level's title
    val storyNextLevelButton: String,
    val countModeLabel: String,
    val countModeSubtitle: String,
    val countHint: String,
    val countRangeLabel: String,         // format with 2 %d = from, to
    val countUpLabel: String,
    val countDownLabel: String,
    val countUpDownLabel: String,
    val countRepeatLabel: String,
    val homeSectionTrain: String,
    val homeSectionListen: String,
    val homeSectionUnderstand: String,
    val homeSectionGames: String,
    val storyTranscriptLabel: String,
)

/** A countable item for story problems; [feminine] lets gendered languages pick agreement. */
data class StoryItem(val plural: String, val feminine: Boolean = false)

/**
 * Everything one language needs for Stories mode (spoken word problems, see
 * [com.dtolan.numbers.logic.StoryGenerator]). Sentences come from [templates], whose `{name}`
 * placeholders the generator fills with pre-formatted values (digits, locale money, clock times);
 * the TTS voice pronounces the numbers. Quantities of [StoryItem]s are always ≥ 2, so only plural
 * forms are needed. Every [StoryKey] must be present — StoryGeneratorTest enforces it.
 */
data class StoryPack(
    val themeTitles: Map<StoryTheme, String>,
    /** One title per level of each theme, in order. */
    val levelTitles: Map<StoryTheme, List<String>>,
    val groceries: List<StoryItem>,
    val cafeItems: List<StoryItem>,
    /** Mass nouns sold by weight at a deli counter ("ham", "cheese"). */
    val deliItems: List<String>,
    /**
     * A picture for each item noun exactly as it appears in the story text ("pommes" → 🍎); "3
     * pommes" is shown as 🍎🍎🍎 as the voice says it. Every grocery, café and deli item needs one (tested).
     */
    val pictures: Map<String, String>,
    val cities: List<String>,
    val streets: List<String>,
    /** Joins phrases into a natural list: "a, b and c". */
    val join: (List<String>) -> String,
    /** Start of a "how many" question with agreement: "Combien de pommes", "Quante mele", "How many apples". */
    val howMany: (StoryItem) -> String,
    /** Plural with its definite article: "les pommes", "gli yogurt", "the apples". */
    val the: (StoryItem) -> String,
    /** A clock time as it sits mid-sentence after the template's preposition: "14 heures 35", "14 e 35", "14:35". */
    val clock: (hour: Int, minute: Int) -> String,
    /** A duration: "1 heure 20", "un'ora e 20 minuti", "1 hour and 20 minutes". */
    val duration: (minutes: Int) -> String,
    val templates: Map<StoryKey, String>,
)

/**
 * Everything the app needs to speak and grade questions in one language.
 *
 * To add a new language: create one more `LanguagePack` instance (see Languages.kt) and
 * register it in `Languages.ALL`. Nothing else in the app needs to change.
 */
data class LanguagePack(
    val code: String,
    val displayName: String,
    val flag: String,
    val locale: Locale,
    val decimalSeparator: Char,
    val currencySymbol: String,
    val currencyBeforeValue: Boolean,
    val percentHasSpace: Boolean,
    val monthNames: List<String>,
    val dateOrder: DateOrder,
    val categoryLabels: Map<Category, String>,
    val ui: UiStrings,
    val ordinalDay: (Int) -> String,
    val speakDate: (day: Int, month0: Int, year: Int, ordinal: String, monthName: String) -> String,
    val speakTime: (hour: Int, minute: Int) -> String,
    val placeholders: Map<Category, String>,
    val phoneNumber: (Random) -> PhoneNumber,
    val learnStages: List<LearnStage> = emptyList(),
    val stories: StoryPack,
    /** A whole number written out in words, for display under the digits (see [NumberWords]). */
    val spellNumber: (Int) -> String,
    /** A year written as it's said — same as [spellNumber] except in English ("nineteen fifty-four"). */
    val spellYear: (Int) -> String = spellNumber,
    /** An ordinal in words — "troisième", "terzo"/"terza", "third"; the flag picks the feminine form. */
    val spellOrdinal: (n: Int, feminine: Boolean) -> String,
    /** An ordinal in figures: "3e", "3º", "3rd". */
    val ordinalLabel: (Int) -> String,
    val speak: SpeakPack,
)

/** A generated phone number: how it should be displayed/spoken, and its bare digits for grading. */
data class PhoneNumber(val display: String, val digits: String)
