package com.dtolan.numbers.data

/**
 * Whole numbers written out in words ("73" → "soixante-treize"), shown under the digits in the
 * passive-listening screens so the learner can tie the sound to its spelling. Speech still comes
 * from handing the TTS voice digits; these are display-only. Covers 0 to 999 999 999 — anything
 * outside falls back to the digits.
 */
object NumberWords {

    // ---- French (traditional spelling: hyphens below 100, "et" in 21…71, plural "vingts"/"cents") ----

    private val FR_UNITS = listOf(
        "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
        "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
    )
    private val FR_TENS = listOf("", "", "vingt", "trente", "quarante", "cinquante", "soixante")

    /** [final]: nothing follows, so "quatre-vingts"/"deux cents" keep their plural s (not before "mille"). */
    private fun frBelow100(n: Int, final: Boolean): String {
        val tens = n / 10
        val unit = n % 10
        return when {
            n < 17 -> FR_UNITS[n]
            n < 20 -> "dix-" + FR_UNITS[unit]
            tens <= 6 -> when (unit) {
                0 -> FR_TENS[tens]
                1 -> FR_TENS[tens] + " et un"
                else -> FR_TENS[tens] + "-" + FR_UNITS[unit]
            }
            tens == 7 -> if (unit == 1) "soixante et onze" else "soixante-" + frBelow100(10 + unit, final)
            tens == 8 -> if (unit == 0) (if (final) "quatre-vingts" else "quatre-vingt") else "quatre-vingt-" + FR_UNITS[unit]
            else -> "quatre-vingt-" + frBelow100(10 + unit, final)
        }
    }

    private fun frBelow1000(n: Int, final: Boolean): String {
        val hundreds = n / 100
        val rest = n % 100
        if (hundreds == 0) return frBelow100(rest, final)
        val head = if (hundreds == 1) "cent" else FR_UNITS[hundreds] + " cent"
        return when {
            rest != 0 -> head + " " + frBelow100(rest, final)
            hundreds > 1 && final -> head + "s"
            else -> head
        }
    }

    fun french(n: Int): String {
        if (n !in 0..999_999_999) return n.toString()
        if (n == 0) return FR_UNITS[0]
        val millions = n / 1_000_000
        val thousands = n / 1000 % 1000
        val rest = n % 1000
        return listOfNotNull(
            millions.takeIf { it > 0 }?.let { frBelow1000(it, final = true) + if (it > 1) " millions" else " million" },
            thousands.takeIf { it > 0 }?.let { if (it == 1) "mille" else frBelow1000(it, final = false) + " mille" },
            rest.takeIf { it > 0 }?.let { frBelow1000(it, final = true) },
        ).joinToString(" ")
    }

    /** "premier"/"première", "deuxième", "vingt et unième", "quatre-vingtième", "centième", "millième". */
    fun frenchOrdinal(n: Int, feminine: Boolean = false): String {
        if (n == 1) return if (feminine) "première" else "premier"
        if (n < 1) return french(n)
        // The ordinal ending replaces the plural s: quatre-vingts → quatre-vingtième.
        val words = french(n).let { if (it.endsWith("vingts") || it.endsWith("cents")) it.dropLast(1) else it }
        return when {
            words.endsWith("un") -> words.dropLast(2) + "unième"
            words.endsWith("cinq") -> words + "uième"
            words.endsWith("neuf") -> words.dropLast(1) + "vième"
            words.endsWith("e") -> words.dropLast(1) + "ième"
            else -> words + "ième"
        }
    }

    // ---- Italian (one word below a million, vowel elision "ventuno"/"centotto", accented "-tré") ----

    private val IT_UNITS = listOf(
        "zero", "uno", "due", "tre", "quattro", "cinque", "sei", "sette", "otto", "nove",
        "dieci", "undici", "dodici", "tredici", "quattordici", "quindici", "sedici",
        "diciassette", "diciotto", "diciannove",
    )
    private val IT_TENS = listOf("", "", "venti", "trenta", "quaranta", "cinquanta", "sessanta", "settanta", "ottanta", "novanta")

    private fun itBelow100(n: Int): String {
        if (n < 20) return IT_UNITS[n]
        val tens = IT_TENS[n / 10]
        val unit = n % 10
        return when (unit) {
            0 -> tens
            1, 8 -> tens.dropLast(1) + IT_UNITS[unit] // ventuno, ventotto
            else -> tens + IT_UNITS[unit]
        }
    }

    private fun itBelow1000(n: Int): String {
        val hundreds = n / 100
        val rest = n % 100
        val head = when (hundreds) {
            0 -> ""
            1 -> "cento"
            else -> IT_UNITS[hundreds] + "cento"
        }
        if (rest == 0) return head
        val tail = itBelow100(rest)
        // centotto, centottanta — but centouno stays.
        return if (head.isNotEmpty() && tail.startsWith("ott")) head.dropLast(1) + tail else head + tail
    }

    /** A compound ending in "tre" takes the accent ("ventitré", "milletré"); bare "tre" doesn't. */
    private fun itAccent(word: String): String =
        if (word.length > 3 && word.endsWith("tre")) word.dropLast(3) + "tré" else word

    fun italian(n: Int): String {
        if (n !in 0..999_999_999) return n.toString()
        if (n == 0) return IT_UNITS[0]
        val millions = n / 1_000_000
        val thousands = n / 1000 % 1000
        val rest = n % 1000
        val millionPart = when {
            millions == 0 -> null
            millions == 1 -> "un milione"
            // ventun milioni: "uno" drops its o here too.
            millions % 10 == 1 && millions % 100 > 20 -> itBelow1000(millions).dropLast(1) + " milioni"
            else -> itAccent(itBelow1000(millions)) + " milioni"
        }
        val thousandPart = when {
            thousands == 0 -> ""
            thousands == 1 -> "mille"
            // ventunmila, trentunmila: "uno" drops its o before "mila".
            thousands % 10 == 1 && thousands % 100 > 20 -> itBelow1000(thousands).dropLast(1) + "mila"
            else -> itBelow1000(thousands) + "mila"
        }
        val belowMillion = (thousandPart + if (rest > 0) itBelow1000(rest) else "").takeIf { it.isNotEmpty() }?.let(::itAccent)
        return listOfNotNull(millionPart, belowMillion).joinToString(" ")
    }

    private val IT_ORDINALS = listOf("", "primo", "secondo", "terzo", "quarto", "quinto", "sesto", "settimo", "ottavo", "nono", "decimo")

    /** "primo", "terzo", "undicesimo", "ventitreesimo", "ventiseiesimo", "centesimo"; [feminine] ends in -a. */
    fun italianOrdinal(n: Int, feminine: Boolean = false): String {
        if (n < 1) return italian(n)
        val words = italian(n)
        val masculine = when {
            n <= 10 -> IT_ORDINALS[n]
            words.endsWith("tré") -> words.dropLast(1) + "eesimo"
            words.endsWith("sei") -> words + "esimo"
            words.endsWith("mila") -> words.dropLast(4) + "millesimo"
            else -> words.dropLast(1) + "esimo"
        }
        return if (feminine) masculine.dropLast(1) + "a" else masculine
    }

    // ---- English (US style: "one hundred twenty-three", no "and") ----

    private val EN_UNITS = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen",
        "seventeen", "eighteen", "nineteen",
    )
    private val EN_TENS = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

    private fun enBelow1000(n: Int): String {
        val hundreds = n / 100
        val rest = n % 100
        val restWords = when {
            rest == 0 -> null
            rest < 20 -> EN_UNITS[rest]
            rest % 10 == 0 -> EN_TENS[rest / 10]
            else -> EN_TENS[rest / 10] + "-" + EN_UNITS[rest % 10]
        }
        return listOfNotNull(hundreds.takeIf { it > 0 }?.let { EN_UNITS[it] + " hundred" }, restWords).joinToString(" ")
    }

    fun english(n: Int): String {
        if (n !in 0..999_999_999) return n.toString()
        if (n == 0) return EN_UNITS[0]
        val millions = n / 1_000_000
        val thousands = n / 1000 % 1000
        val rest = n % 1000
        return listOfNotNull(
            millions.takeIf { it > 0 }?.let { enBelow1000(it) + " million" },
            thousands.takeIf { it > 0 }?.let { enBelow1000(it) + " thousand" },
            rest.takeIf { it > 0 }?.let(::enBelow1000),
        ).joinToString(" ")
    }

    private val EN_IRREGULAR_ORDINALS = mapOf(
        "one" to "first", "two" to "second", "three" to "third", "five" to "fifth",
        "eight" to "eighth", "nine" to "ninth", "twelve" to "twelfth",
    )

    /** "first", "twenty-second", "fortieth", "one hundred third" — only the last word changes. */
    fun englishOrdinalWords(n: Int): String {
        if (n < 1) return english(n)
        val words = english(n)
        val cut = maxOf(words.lastIndexOf(' '), words.lastIndexOf('-')) + 1
        val last = words.substring(cut)
        val ordinal = EN_IRREGULAR_ORDINALS[last] ?: if (last.endsWith("y")) last.dropLast(1) + "ieth" else last + "th"
        return words.substring(0, cut) + ordinal
    }

    /** English reads years in pairs: "nineteen fifty-four", "nineteen oh-five", "twenty twenty-six". */
    fun englishYear(n: Int): String = when {
        n !in 1100..2099 || n in 2000..2009 -> english(n)
        n % 100 == 0 -> english(n / 100) + " hundred"
        n % 100 < 10 -> english(n / 100) + " oh-" + english(n % 100)
        else -> english(n / 100) + " " + english(n % 100)
    }
}

/**
 * Every free-standing whole number in [text] followed by its spelling: "10 pommes" → "10 (dix)
 * pommes". Numbers that are part of something else are left alone — money and decimals ("4,50 €",
 * "$12.50"), clock times ("14:35"), percentages, ordinals ("1er", "3rd"), and phone/digit groups
 * ("06 12 34 56 78", "555-123-4567") — since spelling one fragment of those would mislead.
 * Four-digit numbers from 1100 to 2099 are read as years and use [spellYear].
 */
fun annotateNumberWords(text: String, spell: (Int) -> String, spellYear: (Int) -> String = spell): String =
    STANDALONE_NUMBER.replace(text) { match ->
        val digits = match.value
        // Leading zeros only occur in phone/code groups ("06"); spelling those would be wrong.
        if (digits.length > 1 && digits.startsWith('0') || digits.length > 9) digits
        else if (digits.length == 4 && digits.toInt() in 1100..2099) "$digits (${spellYear(digits.toInt())})"
        else "$digits (${spell(digits.toInt())})"
    }

private val STANDALONE_NUMBER = Regex(
    // Not preceded by a digit (+ separator), a currency sign, a dash or a letter…
    "(?<![\\d\\p{L}€$£/\\-])(?<!\\d[.,:])(?<!\\d[\\s\\u00A0\\u202F])" +
        "\\d+" +
        // …and not followed by more digits, a decimal/time/group separator + digit, %, €, or a letter.
        "(?![\\d\\p{L}%])(?![.,:/\\-]\\d)(?![\\s\\u00A0\\u202F]?[€%])(?![\\s\\u00A0\\u202F]\\d)",
)
