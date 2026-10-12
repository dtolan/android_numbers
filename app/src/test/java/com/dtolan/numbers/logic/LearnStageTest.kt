package com.dtolan.numbers.logic

import com.dtolan.numbers.data.LearnStage
import com.dtolan.numbers.data.Languages
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnStageTest {

    @Test
    fun `range-based stage only draws values within its bounds`() {
        val stage = LearnStage(title = "70-79", description = "", min = 70, max = 79)
        repeat(200) {
            val q = QuestionGenerators.genLearnQuestion(Languages.FRENCH, stage, Random(it))
            val value = q.displayAnswer.toInt()
            assertTrue("$value not in 70..79", value in 70..79)
        }
    }

    @Test
    fun `values-based stage only draws from its explicit list`() {
        val decadeWords = listOf(10, 20, 30, 40, 50, 60, 80, 100)
        val stage = LearnStage(title = "decades", description = "", min = 0, max = 0, values = decadeWords)
        repeat(200) {
            val q = QuestionGenerators.genLearnQuestion(Languages.FRENCH, stage, Random(it))
            assertTrue(q.displayAnswer.toInt() in decadeWords)
        }
    }

    @Test
    fun `every built-in language defines a non-empty, well-formed curriculum`() {
        for (lang in Languages.ALL) {
            assertTrue("${lang.code} has no learn stages", lang.learnStages.isNotEmpty())
            for (stage in lang.learnStages) {
                assertTrue("${lang.code}/${stage.title} requiredStreak must be positive", stage.requiredStreak > 0)
                val values = stage.values
                if (values != null) {
                    assertTrue("${lang.code}/${stage.title} has an empty values list", values.isNotEmpty())
                } else {
                    assertTrue("${lang.code}/${stage.title} has min > max", stage.min <= stage.max)
                }
            }
        }
    }

    @Test
    fun `french curriculum isolates the irregular 70s, 80s and 90s into their own stages`() {
        val titles = Languages.FRENCH.learnStages.map { it.title }
        assertTrue(titles.contains("70–79"))
        assertTrue(titles.contains("80–89"))
        assertTrue(titles.contains("90–99"))
    }

    @Test
    fun `flash-drill choices always include the correct answer exactly once`() {
        val stage = LearnStage(title = "70-79", description = "", min = 70, max = 79)
        repeat(100) {
            val q = QuestionGenerators.genLearnQuestion(Languages.FRENCH, stage, Random(it))
            val choices = requireNotNull(q.choices)
            assertEquals(1, choices.count { c -> c == q.displayAnswer })
        }
    }

    @Test
    fun `flash-drill choices have no duplicates and default to 4 options`() {
        val stage = LearnStage(title = "0-20", description = "", min = 0, max = 20)
        val q = QuestionGenerators.genLearnQuestion(Languages.FRENCH, stage, Random(42))
        val choices = requireNotNull(q.choices)
        assertEquals(4, choices.size)
        assertEquals(choices.size, choices.toSet().size)
    }

    @Test
    fun `flash-drill choice count shrinks gracefully for tiny value pools`() {
        val stage = LearnStage(title = "tiny", description = "", min = 0, max = 0, values = listOf(1, 2))
        val q = QuestionGenerators.genLearnQuestion(Languages.FRENCH, stage, Random(1))
        val choices = requireNotNull(q.choices)
        assertEquals(2, choices.size)
        assertTrue(choices.containsAll(listOf("1", "2")))
    }
}
