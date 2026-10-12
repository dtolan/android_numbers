package com.dtolan.numbers.logic

import com.dtolan.numbers.data.LanguagePack
import java.text.Normalizer
import kotlin.random.Random

/** How a spoken answer is read: a plain number, a year, or an ordinal ("third"). */
enum class AnswerKind { CARDINAL, YEAR, ORDINAL }

/**
 * One Speak-mode task. [prompt] is shown large (digits, "21e", or 🔊 for heard tasks), [speakText]
 * is read aloud by TTS when set, and the learner says [expected] out loud.
 */
data class SpeakTask(
    val instruction: String,
    val prompt: String,
    val speakText: String?,
    val expected: Int,
    val kind: AnswerKind,
) {
    /** The answer in figures, as shown after answering: "78", "21e", "1954". */
    fun answerLabel(lang: LanguagePack): String =
        if (kind == AnswerKind.ORDINAL) lang.ordinalLabel(expected) else expected.toString()

    /**
     * What the voice reads to model the answer: digits for plain numbers (the TTS voice says those
     * reliably), words for ordinals and for years where a language reads them differently.
     */
    fun answerSpeech(lang: LanguagePack): String = when (kind) {
        AnswerKind.CARDINAL -> expected.toString()
        AnswerKind.YEAR -> lang.spellYear(expected).takeIf { it != lang.spellNumber(expected) } ?: expected.toString()
        AnswerKind.ORDINAL -> lang.spellOrdinal(expected, false)
    }

    /** The answer in words: "soixante-dix-huit", "vingt et unième", "nineteen fifty-four". */
    fun answerWords(lang: LanguagePack): String = when (kind) {
        AnswerKind.CARDINAL -> lang.spellNumber(expected)
        AnswerKind.YEAR -> lang.spellYear(expected)
        AnswerKind.ORDINAL -> lang.spellOrdinal(expected, false)
    }
}

/**
 * Speak mode's ladder of levels: say what you see (numbers, big numbers, ordinals, years), then
 * say what you work out from what you hear (next/previous number, mental arithmetic, position
 * puzzles). Every level is open to try in any order.
 */
object SpeakGenerator {
    const val LEVEL_COUNT = 7

    fun generate(level: Int, lang: LanguagePack, random: Random = Random.Default): SpeakTask {
        val s = lang.speak
        return when (level) {
            0 -> random.nextInt(0, 101).let { SpeakTask(s.sayNumber, "$it", null, it, AnswerKind.CARDINAL) }
            1 -> bigNumber(random).let { SpeakTask(s.sayNumber, "$it", null, it, AnswerKind.CARDINAL) }
            2 -> ordinalValue(random).let { SpeakTask(s.sayOrdinal, lang.ordinalLabel(it), null, it, AnswerKind.ORDINAL) }
            3 -> random.nextInt(1900, 2031).let { SpeakTask(s.sayYear, "$it", null, it, AnswerKind.YEAR) }
            4 -> nextOrPrevious(lang, random)
            5 -> mentalMaths(lang, random)
            else -> position(lang, random)
        }
    }

    /** 100–9999, with a third of them on a round hundred or thousand boundary ("2 000", "1 100"). */
    private fun bigNumber(random: Random): Int = when (random.nextInt(3)) {
        0 -> random.nextInt(1, 100) * 100
        1 -> random.nextInt(100, 1000)
        else -> random.nextInt(1000, 10000)
    }

    /** Mostly 1–31 (dates, floors, places), sometimes up to 100. */
    private fun ordinalValue(random: Random): Int =
        if (random.nextInt(4) == 0) random.nextInt(32, 101) else random.nextInt(1, 32)

    private fun nextOrPrevious(lang: LanguagePack, random: Random): SpeakTask {
        val next = random.nextBoolean()
        // Half the time land on a tens boundary — 69→70, 79→80, 99→100 are where it hurts.
        val n = if (random.nextBoolean()) {
            val tens = random.nextInt(1, 10) * 10
            if (next) tens - 1 else tens
        } else {
            random.nextInt(1, 100)
        }
        val expected = if (next) n + 1 else n - 1
        return SpeakTask(if (next) lang.speak.sayNext else lang.speak.sayPrevious, "🔊", "$n", expected, AnswerKind.CARDINAL)
    }

    private fun mentalMaths(lang: LanguagePack, random: Random): SpeakTask {
        // Bias the answers into 60–99, the hardest stretch to say in French.
        val result = if (random.nextBoolean()) random.nextInt(60, 100) else random.nextInt(10, 101)
        val text = if (random.nextBoolean()) {
            val b = random.nextInt(2, result.coerceAtMost(40))
            "${result - b} ${lang.speak.plus} $b"
        } else {
            val b = random.nextInt(2, 41)
            "${result + b} ${lang.speak.minus} $b"
        }
        return SpeakTask(lang.speak.sayResult, "🔊", text, result, AnswerKind.CARDINAL)
    }

    private fun position(lang: LanguagePack, random: Random): SpeakTask {
        val s = lang.speak
        val (text, expected) = when (random.nextInt(3)) {
            0 -> {
                val place = random.nextInt(2, 21)
                s.raceTemplate.replace("{ord}", lang.spellOrdinal(place, s.raceFeminine)) to place
            }
            1 -> {
                val place = random.nextInt(5, 21)
                val left = random.nextInt(2, place - 1)
                fill(s.queueTemplate, lang.spellOrdinal(place, false), left) to place - left
            }
            else -> {
                val floor = random.nextInt(1, 10)
                val up = random.nextInt(2, 8)
                fill(s.floorTemplate, lang.spellOrdinal(floor, false), up) to floor + up
            }
        }
        return SpeakTask(s.sayPosition, "🔊", text, expected, AnswerKind.ORDINAL)
    }

    private fun fill(template: String, ordinal: String, k: Int) =
        template.replace("{ord}", ordinal).replace("{k}", "$k")
}

/**
 * Grades what the speech recognizer heard against a [SpeakTask]. The recognizer returns several
 * guesses, sometimes as digits ("92", "1 954", "3e", "21st"), sometimes as words ("quatre-vingt-
 * douze", "the third") — any guess that matches counts. Words are compared with spaces, hyphens,
 * accents and a leading article stripped, so "vingt et un", "vingt-et-un" and "le vingt et unième"
 * all line up with how [NumberWords] spells them.
 */
object SpeechGrader {

    fun matches(task: SpeakTask, heard: List<String>, lang: LanguagePack): Boolean =
        heard.any { matchesOne(task, it, lang) }

    private fun matchesOne(task: SpeakTask, raw: String, lang: LanguagePack): Boolean {
        val text = stripArticle(raw.trim().lowercase(lang.locale), lang)
        return when (task.kind) {
            AnswerKind.CARDINAL, AnswerKind.YEAR -> {
                digitsOnly(text) == task.expected ||
                    acceptedWords(task, lang).any { squash(it, lang) == squash(text, lang) }
            }
            AnswerKind.ORDINAL -> {
                ordinalFigures(text) == task.expected ||
                    acceptedWords(task, lang).any { squash(it, lang) == squash(text, lang) }
            }
        }
    }

    private fun acceptedWords(task: SpeakTask, lang: LanguagePack): List<String> = when (task.kind) {
        // A year said as a plain number ("mille neuf cent…", "one thousand nine hundred…") is fine too.
        AnswerKind.YEAR -> listOf(lang.spellYear(task.expected), lang.spellNumber(task.expected))
        AnswerKind.CARDINAL -> listOf(lang.spellNumber(task.expected)) +
            HOMOPHONES[lang.code]?.get(task.expected).orEmpty()
        AnswerKind.ORDINAL -> buildList {
            add(lang.spellOrdinal(task.expected, false))
            add(lang.spellOrdinal(task.expected, true))
            // French also has "second(e)" for 2nd.
            if (lang.code == "fr" && task.expected == 2) { add("second"); add("seconde") }
        }
    }

    /** "92", "1 954", "1.954", "1,954" → the number; anything with letters → null. */
    private fun digitsOnly(text: String): Int? =
        text.takeIf { it.isNotEmpty() && it.all { c -> c.isDigit() || c in " .,  " } }
            ?.filter(Char::isDigit)?.takeIf { it.isNotEmpty() && it.length <= 9 }?.toInt()

    /** "3e", "3ème", "1er", "1re", "3º", "3°", "3rd", "21st" → the number. */
    private fun ordinalFigures(text: String): Int? =
        ORDINAL_FIGURES.matchEntire(text.replace(" ", ""))?.groupValues?.get(1)?.toIntOrNull()

    private fun stripArticle(text: String, lang: LanguagePack): String {
        val articles = when (lang.code) {
            "fr" -> listOf("le ", "la ", "l'", "l’")
            "it" -> listOf("il ", "lo ", "la ", "l'", "l’")
            else -> listOf("the ")
        }
        return articles.firstOrNull { text.startsWith(it) }?.let { text.removePrefix(it) } ?: text
    }

    /** Lowercase letters and digits only, accents folded, French plural vingts/cents and English "and" dropped. */
    private fun squash(text: String, lang: LanguagePack): String {
        var t = text.lowercase(lang.locale)
        if (lang.code == "en") t = t.replace(Regex("\\band\\b"), " ")
        if (lang.code == "fr") t = t.replace("vingts", "vingt").replace("cents", "cent")
        t = Normalizer.normalize(t, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
        return t.filter { it.isLetterOrDigit() }
    }

    /**
     * A number said on its own is often transcribed as a sound-alike word: "dix" → "dis"/"dit",
     * "cent" → "sans", "four" → "for". Only a whole answer that is exactly one of these counts.
     */
    private val HOMOPHONES: Map<String, Map<Int, List<String>>> = mapOf(
        "fr" to mapOf(
            1 to listOf("hein", "un."), 2 to listOf("de", "d'eux"), 5 to listOf("saint", "sain", "sein"),
            6 to listOf("si", "scie"), 7 to listOf("cette", "set"), 10 to listOf("dis", "dit", "dits"),
            20 to listOf("vin", "vain", "vint"), 100 to listOf("sans", "sang", "s'en"), 1000 to listOf("mil"),
        ),
        "en" to mapOf(
            1 to listOf("won"), 2 to listOf("to", "too"), 4 to listOf("for", "fore"), 8 to listOf("ate"),
        ),
    )

    private val ORDINAL_FIGURES = Regex("(\\d{1,4})(e|er|re|ère|ere|ème|eme|e\\.|º|°|o|a|ª|st|nd|rd|th)")
}
