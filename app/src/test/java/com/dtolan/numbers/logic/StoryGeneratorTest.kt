package com.dtolan.numbers.logic

import com.dtolan.numbers.data.Category
import com.dtolan.numbers.data.Languages
import com.dtolan.numbers.data.StoryKey
import com.dtolan.numbers.data.StoryTheme
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryGeneratorTest {

    private val en = Languages.ENGLISH

    private fun numbersIn(text: String) = Regex("\\d+").findAll(text).map { it.value.toInt() }.toList()

    @Test
    fun `every language defines every template, theme title and level title`() {
        Languages.ALL.forEach { lang ->
            val s = lang.stories
            StoryKey.entries.forEach { key -> assertTrue("${lang.code} missing $key", key in s.templates) }
            StoryTheme.entries.forEach { theme ->
                assertTrue("${lang.code} missing title for $theme", theme in s.themeTitles)
                assertEquals("${lang.code} $theme level titles", StoryGenerator.levelCount(theme), s.levelTitles.getValue(theme).size)
            }
        }
    }

    @Test
    fun `every level of every theme in every language grades its own answer and fills every placeholder`() {
        Languages.ALL.forEach { lang ->
            StoryTheme.entries.forEach { theme ->
                for (level in 0 until StoryGenerator.levelCount(theme)) {
                    repeat(40) { seed ->
                        val story = StoryGenerator.generate(theme, level, lang, Random(seed))
                        val q = story.question
                        val ctx = "${lang.code} $theme L$level seed $seed: ${q.speakText}"
                        assertTrue(ctx, q.checkAnswer(q.displayAnswer))
                        assertFalse(ctx, q.checkAnswer("99999"))
                        assertFalse(ctx, q.speakText.contains('{') || q.speakText.contains('}') || q.speakText.contains("null"))
                        assertEquals(ctx, q.speakText, story.lines.joinToString(" ") { it.text })
                    }
                }
            }
        }
    }

    @Test
    fun `first shopping level matches the original example`() {
        val story = StoryGenerator.generate(StoryTheme.SHOPPING, 0, en, Random(7))
        val text = story.question.speakText
        assertTrue(text, text.startsWith("You're going to the store to pick up "))
        val item = Regex("How many (\\w+) do you need\\?").find(text)!!.groupValues[1]
        assertTrue(text, text.contains("${story.question.displayAnswer} $item"))
    }

    @Test
    fun `recall levels never repeat a quantity`() {
        repeat(50) { seed ->
            listOf(0, 1).forEach { level ->
                val first = StoryGenerator.generate(StoryTheme.SHOPPING, level, en, Random(seed)).lines.first().text
                val qtys = numbersIn(first)
                assertEquals(first, qtys.size, qtys.toSet().size)
            }
        }
    }

    @Test
    fun `totals answer is the sum of the spoken quantities`() {
        repeat(20) { seed ->
            val story = StoryGenerator.generate(StoryTheme.SHOPPING, 2, en, Random(seed))
            assertEquals(story.question.speakText, numbersIn(story.lines.first().text).sum().toString(), story.question.displayAnswer)
        }
    }

    @Test
    fun `corrections ask for the corrected number, not the original`() {
        repeat(30) { seed ->
            val story = StoryGenerator.generate(StoryTheme.SHOPPING, 6, en, Random(seed))
            val original = numbersIn(story.lines[0].text)
            val corrected = numbersIn(story.lines[1].text).single()
            assertEquals(story.question.speakText, corrected.toString(), story.question.displayAnswer)
            assertFalse(story.question.speakText, corrected in original)
        }
    }

    @Test
    fun `door code correction swaps digits and grades the second code`() {
        repeat(30) { seed ->
            val story = StoryGenerator.generate(StoryTheme.HOME, 4, en, Random(seed))
            val text = story.lines.first().text
            val codes = Regex("((?:\\d ){3}\\d)").findAll(text).map { it.value.replace(" ", "") }.toList()
            assertEquals(text, 2, codes.size)
            assertNotEquals(text, codes[0], codes[1])
            assertEquals(text, codes[0].toList().sorted(), codes[1].toList().sorted())
            assertEquals(codes[1], story.question.displayAnswer)
            assertFalse(story.question.checkAnswer(codes[0]))
        }
    }

    @Test
    fun `pharmacist boxes round up`() {
        repeat(30) { seed ->
            val story = StoryGenerator.generate(StoryTheme.HEALTH, 4, en, Random(seed))
            val (perDose, days) = numbersIn(story.lines[0].text)
            val box = numbersIn(story.lines[2].text).single()
            val expected = (perDose * 2 * days + box - 1) / box
            assertEquals(story.question.speakText, expected.toString(), story.question.displayAnswer)
        }
    }

    @Test
    fun `dialogue levels use two voices besides the narrator`() {
        listOf(StoryTheme.SHOPPING to 8, StoryTheme.CAFE to 4, StoryTheme.TRAVEL to 5, StoryTheme.HEALTH to 4).forEach { (theme, level) ->
            val speakers = StoryGenerator.generate(theme, level, en, Random(1)).lines.map { it.speaker }.toSet()
            assertTrue("$theme L$level: $speakers", Speaker.YOU in speakers && Speaker.OTHER in speakers)
        }
    }

    @Test
    fun `change answers are positive and below the note`() {
        Languages.ALL.forEach { lang ->
            listOf(StoryTheme.SHOPPING to 5, StoryTheme.SHOPPING to 7, StoryTheme.CAFE to 4, StoryTheme.HEALTH to 2).forEach { (theme, level) ->
                repeat(20) { seed ->
                    val q = StoryGenerator.generate(theme, level, lang, Random(seed)).question
                    assertEquals(Category.MONEY, q.category)
                    val change = normalizeNumberInput(q.displayAnswer, lang).toDouble()
                    assertTrue(q.speakText, change > 0 && change < 100)
                }
            }
        }
    }

    @Test
    fun `typed answer variants are accepted`() {
        val money = StoryGenerator.generate(StoryTheme.SHOPPING, 5, Languages.FRENCH, Random(3)).question
        assertTrue(money.checkAnswer(money.displayAnswer.replace(" €", "")))
        assertTrue(money.checkAnswer(money.displayAnswer.replace(",", ".")))
        val time = StoryGenerator.generate(StoryTheme.TRAVEL, 2, Languages.FRENCH, Random(3)).question
        assertTrue(time.checkAnswer(time.displayAnswer.replace(":", "h")))
        val date = StoryGenerator.generate(StoryTheme.APPOINTMENTS, 4, Languages.FRENCH, Random(3)).question
        assertTrue(date.checkAnswer(date.displayAnswer.replace("/", ".")))
    }

    @Test
    fun `gender agreement, elision and articles`() {
        val it = Languages.ITALIAN.stories
        fun itItem(name: String) = (it.groceries + it.cafeItems).first { i -> i.plural == name }
        assertEquals("Quanti pomodori", it.howMany(itItem("pomodori")))
        assertEquals("Quante mele", it.howMany(itItem("mele")))
        assertEquals("gli yogurt", it.the(itItem("yogurt")))
        assertEquals("i limoni", it.the(itItem("limoni")))
        assertEquals("le uova", it.the(itItem("uova")))
        val fr = Languages.FRENCH.stories
        fun frItem(name: String) = (fr.groceries + fr.cafeItems).first { i -> i.plural == name }
        assertEquals("Combien d'oranges", fr.howMany(frItem("oranges")))
        assertEquals("Combien de bananes", fr.howMany(frItem("bananes")))
        assertEquals("Combien de yaourts", fr.howMany(frItem("yaourts")))
    }

    @Test
    fun `every item has a picture and counted items become cues`() {
        for (lang in Languages.ALL) {
            val pack = lang.stories
            (pack.groceries + pack.cafeItems).forEach { assertTrue("${lang.code} ${it.plural}", it.plural in pack.pictures) }
            pack.deliItems.forEach { assertTrue("${lang.code} $it", it in pack.pictures) }
        }
        val story = Story(
            lines = listOf(
                StoryLine(Speaker.NARRATOR, "Vai al negozio a comprare 3 mele e 12 pere."),
                StoryLine(Speaker.NARRATOR, "Quante pere ti servono?"),
            ),
            question = StoryGenerator.generate(StoryTheme.SHOPPING, 0, Languages.ITALIAN, Random(1)).question,
        )
        val cues = pictureCues(story, Languages.ITALIAN.stories)
        assertEquals(listOf(3 to "🍎", 12 to "🍐"), cues.map { it.count to it.picture })
        assertEquals(story.lines[0].text.indexOf("3 mele"), cues[0].start)
        // Prices and weights are not counts.
        val deli = Story(listOf(StoryLine(Speaker.NARRATOR, "Tu demandes 400 grammes de jambon à 1,50 € pièce et 2 cafés.")), story.question)
        assertEquals(listOf(2 to "☕"), pictureCues(deli, Languages.FRENCH.stories).map { it.count to it.picture })
    }
}
