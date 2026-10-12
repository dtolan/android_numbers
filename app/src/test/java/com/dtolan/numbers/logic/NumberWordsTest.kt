package com.dtolan.numbers.logic

import com.dtolan.numbers.data.NumberWords
import com.dtolan.numbers.data.annotateNumberWords
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberWordsTest {

    @Test
    fun `french spells the irregular decades and plural agreement`() {
        val cases = mapOf(
            0 to "zéro", 16 to "seize", 17 to "dix-sept", 21 to "vingt et un", 22 to "vingt-deux",
            70 to "soixante-dix", 71 to "soixante et onze", 77 to "soixante-dix-sept",
            80 to "quatre-vingts", 81 to "quatre-vingt-un", 90 to "quatre-vingt-dix", 91 to "quatre-vingt-onze",
            99 to "quatre-vingt-dix-neuf", 100 to "cent", 101 to "cent un", 200 to "deux cents",
            280 to "deux cent quatre-vingts", 1000 to "mille", 1984 to "mille neuf cent quatre-vingt-quatre",
            2000 to "deux mille", 21_000 to "vingt et un mille", 80_000 to "quatre-vingt mille",
            200_000 to "deux cent mille", 1_000_000 to "un million", 2_500_000 to "deux millions cinq cent mille",
        )
        cases.forEach { (n, words) -> assertEquals("$n", words, NumberWords.french(n)) }
    }

    @Test
    fun `italian elides vowels and accents compound tre`() {
        val cases = mapOf(
            0 to "zero", 3 to "tre", 17 to "diciassette", 21 to "ventuno", 23 to "ventitré", 28 to "ventotto",
            100 to "cento", 101 to "centouno", 108 to "centotto", 180 to "centottanta", 333 to "trecentotrentatré",
            1000 to "mille", 1003 to "milletré", 1984 to "millenovecentottantaquattro", 2000 to "duemila",
            21_000 to "ventunmila", 23_000 to "ventitremila", 1_000_000 to "un milione",
            21_000_000 to "ventun milioni", 3_000_003 to "tre milioni tre", 101_000 to "centounomila",
        )
        cases.forEach { (n, words) -> assertEquals("$n", words, NumberWords.italian(n)) }
    }

    @Test
    fun `english uses hyphens and no and`() {
        val cases = mapOf(
            0 to "zero", 13 to "thirteen", 40 to "forty", 42 to "forty-two", 100 to "one hundred",
            115 to "one hundred fifteen", 1984 to "one thousand nine hundred eighty-four",
            10_000 to "ten thousand", 1_000_001 to "one million one",
        )
        cases.forEach { (n, words) -> assertEquals("$n", words, NumberWords.english(n)) }
    }

    @Test
    fun `out of range falls back to digits`() {
        assertEquals("-5", NumberWords.french(-5))
        assertEquals("1000000000", NumberWords.english(1_000_000_000))
    }

    @Test
    fun `english years read in pairs`() {
        assertEquals("nineteen fifty-four", NumberWords.englishYear(1954))
        assertEquals("nineteen oh-five", NumberWords.englishYear(1905))
        assertEquals("nineteen hundred", NumberWords.englishYear(1900))
        assertEquals("two thousand five", NumberWords.englishYear(2005))
        assertEquals("twenty twenty-six", NumberWords.englishYear(2026))
        assertEquals("nine hundred eighty", NumberWords.englishYear(980))
    }

    @Test
    fun `transcript spells standalone counts only`() {
        fun fr(text: String) = annotateNumberWords(text, NumberWords::french)
        assertEquals("acheter 10 (dix) pommes et 2 (deux) poires.", fr("acheter 10 pommes et 2 poires."))
        assertEquals("3 (trois) poires à 0,50 € pièce", fr("3 poires à 0,50 € pièce"))
        assertEquals("un billet de 20 €", fr("un billet de 20 €"))
        assertEquals("part à 16 (seize) heures 25 (vingt-cinq).", fr("part à 16 heures 25."))
        assertEquals("au 03 57 69 95 53", fr("au 03 57 69 95 53"))
        assertEquals("le code est 7 8 4 5", fr("le code est 7 8 4 5"))
        assertEquals("le 1er novembre", fr("le 1er novembre"))
        assertEquals("au 7e étage", fr("au 7e étage"))
        assertEquals("6 (six)… non, 8 (huit)", fr("6… non, 8"))
        assertEquals("20 %", fr("20 %"))
        fun en(text: String) = annotateNumberWords(text, NumberWords::english, NumberWords::englishYear)
        assertEquals("at 16:25 for \$23.50", en("at 16:25 for \$23.50"))
        assertEquals("call 677-711-9669", en("call 677-711-9669"))
        assertEquals("born in 1954 (nineteen fifty-four), 3 (three) kids", en("born in 1954, 3 kids"))
    }
}
