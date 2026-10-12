package com.dtolan.numbers.logic

import com.dtolan.numbers.data.Languages
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionGeneratorsTest {

    private val settings = PracticeSettings(minVal = 0, maxVal = 100, decimalPlaces = 2, yearMin = 1950, yearMax = 2026)

    @Test
    fun `integer question accepts the exact value and rejects others`() {
        val q = QuestionGenerators.genInteger(Languages.FRENCH, settings, Random(1))
        assertTrue(q.checkAnswer(q.displayAnswer))
        assertFalse(q.checkAnswer((q.displayAnswer.toInt() + 1).toString()))
    }

    @Test
    fun `decimal question accepts locale comma and rejects wrong value`() {
        val q = QuestionGenerators.genDecimal(Languages.FRENCH, settings, Random(2))
        assertTrue(q.checkAnswer(q.displayAnswer))
        assertFalse(q.checkAnswer("999,99"))
    }

    @Test
    fun `english decimal uses a period separator`() {
        val q = QuestionGenerators.genDecimal(Languages.ENGLISH, settings, Random(2))
        assertFalse(q.displayAnswer.contains(','))
        assertTrue(q.checkAnswer(q.displayAnswer))
    }

    @Test
    fun `money question strips currency symbol and separators`() {
        val q = QuestionGenerators.genMoney(Languages.FRENCH, settings, Random(3))
        assertTrue(q.checkAnswer(q.displayAnswer))
        assertTrue(q.checkAnswer(q.displayAnswer.replace(" ", "").replace("€", "")))
    }

    @Test
    fun `english money is prefixed with dollar sign`() {
        val q = QuestionGenerators.genMoney(Languages.ENGLISH, settings, Random(3))
        assertTrue(q.speakText.startsWith("$"))
    }

    @Test
    fun `date question accepts its own display answer`() {
        val q = QuestionGenerators.genDate(Languages.FRENCH, settings, Random(4))
        assertTrue(q.checkAnswer(q.displayAnswer))
    }

    @Test
    fun `english date is month-day-year while french is day-month-year`() {
        val fr = QuestionGenerators.genDate(Languages.FRENCH, settings, Random(4))
        val en = QuestionGenerators.genDate(Languages.ENGLISH, settings, Random(4))
        // Same seed => same day/month/year drawn, but formatted in each language's own order.
        val frParts = fr.displayAnswer.split("/")
        val enParts = en.displayAnswer.split("/")
        assertEquals(frParts[0], enParts[1]) // day
        assertEquals(frParts[1], enParts[0]) // month
        assertEquals(frParts[2], enParts[2]) // year
    }

    @Test
    fun `percent question accepts trailing percent sign`() {
        val q = QuestionGenerators.genPercent(Languages.FRENCH, settings, Random(5))
        assertTrue(q.checkAnswer(q.displayAnswer))
    }

    @Test
    fun `phone question grades on digits only regardless of spacing`() {
        val q = QuestionGenerators.genPhone(Languages.ITALIAN, Random(6))
        assertTrue(q.checkAnswer(q.digitsOnly()))
        assertTrue(q.checkAnswer(q.displayAnswer))
    }

    @Test
    fun `time question accepts colon and h separated input`() {
        val q = QuestionGenerators.genTime(Languages.FRENCH, settings, Random(7))
        assertTrue(q.checkAnswer(q.displayAnswer))
        val hAndM = q.displayAnswer.replace(":", "h")
        assertTrue(q.checkAnswer(hAndM))
    }

    private fun Question.digitsOnly(): String = displayAnswer.filter { it.isDigit() }
}
