package com.dtolan.numbers.logic

import com.dtolan.numbers.data.Languages
import com.dtolan.numbers.data.NumberWords
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakGeneratorTest {

    private val fr = Languages.FRENCH
    private val it = Languages.ITALIAN
    private val en = Languages.ENGLISH

    @Test
    fun `ordinals are spelled in all three languages`() {
        mapOf(
            1 to "premier", 2 to "deuxième", 3 to "troisième", 4 to "quatrième", 5 to "cinquième", 9 to "neuvième",
            11 to "onzième", 21 to "vingt et unième", 71 to "soixante et onzième", 80 to "quatre-vingtième",
            81 to "quatre-vingt-unième", 100 to "centième", 200 to "deux centième", 1000 to "millième",
        ).forEach { (n, w) -> assertEquals("$n", w, NumberWords.frenchOrdinal(n)) }
        assertEquals("première", NumberWords.frenchOrdinal(1, feminine = true))

        mapOf(
            1 to "primo", 3 to "terzo", 10 to "decimo", 11 to "undicesimo", 20 to "ventesimo", 21 to "ventunesimo",
            23 to "ventitreesimo", 26 to "ventiseiesimo", 28 to "ventottesimo", 100 to "centesimo",
            1000 to "millesimo", 2000 to "duemillesimo",
        ).forEach { (n, w) -> assertEquals("$n", w, NumberWords.italianOrdinal(n)) }
        assertEquals("terza", NumberWords.italianOrdinal(3, feminine = true))
        assertEquals("ventitreesima", NumberWords.italianOrdinal(23, feminine = true))

        mapOf(
            1 to "first", 2 to "second", 3 to "third", 5 to "fifth", 8 to "eighth", 9 to "ninth", 12 to "twelfth",
            20 to "twentieth", 21 to "twenty-first", 40 to "fortieth", 103 to "one hundred third",
        ).forEach { (n, w) -> assertEquals("$n", w, NumberWords.englishOrdinalWords(n)) }
    }

    @Test
    fun `every level generates gradable tasks in every language`() {
        for (lang in Languages.ALL) for (level in 0 until SpeakGenerator.LEVEL_COUNT) repeat(50) { seed ->
            val task = SpeakGenerator.generate(level, lang, Random(seed))
            assertTrue("$lang $level", task.expected >= 0)
            assertFalse("${lang.code} $level ${task.speakText}", task.speakText.orEmpty().contains('{'))
            // Saying the answer in words, or the recognizer writing it in figures, both count.
            assertTrue(SpeechGrader.matches(task, listOf(task.answerWords(lang)), lang))
            assertTrue(SpeechGrader.matches(task, listOf(task.answerLabel(lang)), lang))
        }
    }

    private fun cardinal(n: Int) = SpeakTask("", "$n", null, n, AnswerKind.CARDINAL)
    private fun ordinal(n: Int) = SpeakTask("", "", null, n, AnswerKind.ORDINAL)
    private fun year(n: Int) = SpeakTask("", "$n", null, n, AnswerKind.YEAR)

    @Test
    fun `grader accepts the recognizer's usual spellings`() {
        assertTrue(SpeechGrader.matches(cardinal(92), listOf("quatre vingt douze"), fr))
        assertTrue(SpeechGrader.matches(cardinal(92), listOf("Quatre-vingt-douze."), fr))
        assertTrue(SpeechGrader.matches(cardinal(80), listOf("quatre-vingt"), fr))
        assertTrue(SpeechGrader.matches(cardinal(1954), listOf("1 954"), fr))
        assertTrue(SpeechGrader.matches(cardinal(23), listOf("venti tre"), it))
        assertTrue(SpeechGrader.matches(cardinal(105), listOf("one hundred and five"), en))
        // Any of the recognizer's guesses may be the right one.
        assertTrue(SpeechGrader.matches(cardinal(70), listOf("60", "70"), fr))

        assertTrue(SpeechGrader.matches(ordinal(3), listOf("3e"), fr))
        assertTrue(SpeechGrader.matches(ordinal(3), listOf("3ème"), fr))
        assertTrue(SpeechGrader.matches(ordinal(1), listOf("la première"), fr))
        assertTrue(SpeechGrader.matches(ordinal(2), listOf("second"), fr))
        assertTrue(SpeechGrader.matches(ordinal(3), listOf("terza"), it))
        assertTrue(SpeechGrader.matches(ordinal(3), listOf("3º"), it))
        assertTrue(SpeechGrader.matches(ordinal(21), listOf("the twenty first"), en))
        assertTrue(SpeechGrader.matches(ordinal(21), listOf("21st"), en))

        assertTrue(SpeechGrader.matches(year(1954), listOf("nineteen fifty four"), en))
        assertTrue(SpeechGrader.matches(year(1954), listOf("1954"), en))
        assertTrue(SpeechGrader.matches(year(1905), listOf("nineteen oh five"), en))
    }

    @Test
    fun `grader rejects near misses`() {
        assertFalse(SpeechGrader.matches(cardinal(92), listOf("82"), fr))
        assertFalse(SpeechGrader.matches(cardinal(92), listOf("quatre vingt deux"), fr))
        assertFalse(SpeechGrader.matches(cardinal(1), listOf("vingt et un"), fr))
        // A bare cardinal is not an ordinal answer.
        assertFalse(SpeechGrader.matches(ordinal(3), listOf("3"), fr))
        assertFalse(SpeechGrader.matches(ordinal(3), listOf("trois"), fr))
        assertFalse(SpeechGrader.matches(ordinal(3), listOf("three"), en))
    }

    @Test
    fun `position puzzles compute the right place`() {
        repeat(100) { seed ->
            val task = SpeakGenerator.generate(6, en, Random(seed))
            val text = task.speakText!!
            val words = (1..30).associateBy { NumberWords.englishOrdinalWords(it) }
            val given = words.entries.first { (w, _) -> text.contains(" $w ") }.value
            val k = Regex("(\\d+) (people|floors)").find(text)?.groupValues?.get(1)?.toInt()
            val expected = when {
                text.startsWith("In a race") -> given
                text.contains("in line") -> given - k!!
                else -> given + k!!
            }
            assertEquals(text, expected, task.expected)
            assertTrue(text, task.expected >= 1)
        }
    }

    @Test
    fun `model answer reads digits for numbers and words for ordinals and english years`() {
        assertEquals("92", cardinal(92).answerSpeech(fr))
        assertEquals("vingt et unième", ordinal(21).answerSpeech(fr))
        assertEquals("terzo", ordinal(3).answerSpeech(it))
        assertEquals("nineteen fifty-four", year(1954).answerSpeech(en))
        assertEquals("1954", year(1954).answerSpeech(fr))
    }

    @Test
    fun `short numbers accept their sound-alikes`() {
        assertTrue(SpeechGrader.matches(cardinal(10), listOf("dis"), fr))
        assertTrue(SpeechGrader.matches(cardinal(10), listOf("Dit"), fr))
        assertTrue(SpeechGrader.matches(cardinal(100), listOf("sans"), fr))
        assertTrue(SpeechGrader.matches(cardinal(4), listOf("for"), en))
        // Only the whole answer — "dis" inside a sentence, or for another number, doesn't count.
        assertFalse(SpeechGrader.matches(cardinal(10), listOf("dis moi"), fr))
        assertFalse(SpeechGrader.matches(cardinal(6), listOf("dis"), fr))
    }
}
