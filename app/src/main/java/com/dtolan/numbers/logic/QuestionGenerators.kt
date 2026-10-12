package com.dtolan.numbers.logic

import com.dtolan.numbers.data.Category
import com.dtolan.numbers.data.DateOrder
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.LearnStage
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

/** Strips currency symbols/whitespace and converts a locale decimal separator to '.'. */
fun normalizeNumberInput(input: String, lang: LanguagePack): String {
    return input
        .trim()
        .replace(Regex("[\\s€$%]"), "")
        .replace(lang.decimalSeparator, '.')
}

private fun randInt(min: Int, max: Int, random: Random): Int =
    random.nextInt(minOf(min, max), maxOf(min, max) + 1)

object QuestionGenerators {

    fun generate(category: Category, lang: LanguagePack, settings: PracticeSettings, random: Random = Random.Default): Question =
        when (category) {
            Category.INTEGER -> genInteger(lang, settings, random)
            Category.DECIMAL -> genDecimal(lang, settings, random)
            Category.MONEY -> genMoney(lang, settings, random)
            Category.DATE -> genDate(lang, settings, random)
            Category.PERCENT -> genPercent(lang, settings, random)
            Category.PHONE -> genPhone(lang, random)
            Category.TIME -> genTime(lang, settings, random)
        }

    fun genInteger(lang: LanguagePack, settings: PracticeSettings, random: Random = Random.Default): Question =
        genIntegerValue(lang, randInt(settings.minVal, settings.maxVal, random))

    /** A [Category.INTEGER] question for one specific, pre-chosen value. */
    fun genIntegerValue(lang: LanguagePack, value: Int): Question {
        return Question(
            category = Category.INTEGER,
            label = lang.categoryLabels.getValue(Category.INTEGER),
            speakText = value.toString(),
            displayAnswer = value.toString(),
            placeholder = lang.placeholders.getValue(Category.INTEGER),
            checkAnswer = { input -> normalizeNumberInput(input, lang).toIntOrNull() == value },
        )
    }

    /**
     * Draws a value from a guided-learning [LearnStage] (its explicit value list, or its range)
     * and attaches flash-drill [Question.choices]: the correct value plus [choiceCount]-1
     * distractors, preferring "confusable" neighbors (±1, ±10) over arbitrary ones from the stage.
     */
    fun genLearnQuestion(lang: LanguagePack, stage: LearnStage, random: Random = Random.Default, choiceCount: Int = 4): Question {
        val value = stage.values?.let { it[random.nextInt(it.size)] } ?: randInt(stage.min, stage.max, random)
        val pool = (stage.values ?: (stage.min..stage.max).toList()).filter { it != value }
        val preferred = listOf(value - 1, value + 1, value - 10, value + 10).filter { it in pool }
        val distractorCount = (choiceCount - 1).coerceAtMost(pool.size)
        val distractors = (preferred.shuffled(random) + pool.shuffled(random)).distinct().take(distractorCount)
        val choices = (distractors + value).map { it.toString() }.shuffled(random)
        return genIntegerValue(lang, value).copy(choices = choices)
    }

    fun genDecimal(lang: LanguagePack, settings: PracticeSettings, random: Random = Random.Default): Question {
        val places = settings.decimalPlaces.coerceIn(1, 3)
        val lo = minOf(settings.minVal, settings.maxVal)
        val hi = maxOf(settings.minVal, settings.maxVal)
        val scale = 10.0.pow(places)
        val raw = randInt((lo * scale).toInt(), (hi * scale).toInt(), random) / scale
        val value = raw.roundTo(places)
        val spoken = value.formatFixed(places).replace('.', lang.decimalSeparator)
        return Question(
            category = Category.DECIMAL,
            label = lang.categoryLabels.getValue(Category.DECIMAL),
            speakText = spoken,
            displayAnswer = spoken,
            placeholder = lang.placeholders.getValue(Category.DECIMAL),
            checkAnswer = { input ->
                val n = normalizeNumberInput(input, lang).toDoubleOrNull()
                n != null && abs(n - value) < 1e-9
            },
        )
    }

    fun genMoney(lang: LanguagePack, settings: PracticeSettings, random: Random = Random.Default): Question {
        val lo = minOf(settings.minVal, settings.maxVal)
        val hi = maxOf(settings.minVal, settings.maxVal)
        val cents = randInt(lo * 100, hi * 100, random)
        val value = cents / 100.0
        val spoken = formatMoney(cents, lang)
        return Question(
            category = Category.MONEY,
            label = lang.categoryLabels.getValue(Category.MONEY),
            speakText = spoken,
            displayAnswer = spoken,
            placeholder = lang.placeholders.getValue(Category.MONEY),
            checkAnswer = { input ->
                val n = normalizeNumberInput(input, lang).toDoubleOrNull()
                n != null && abs(n - value) < 1e-9
            },
        )
    }

    fun genDate(lang: LanguagePack, settings: PracticeSettings, random: Random = Random.Default): Question {
        val year = randInt(settings.yearMin, settings.yearMax, random)
        val month0 = randInt(0, 11, random)
        val daysInMonth = daysInMonth(year, month0)
        val day = randInt(1, daysInMonth, random)
        val ordinal = lang.ordinalDay(day)
        val monthName = lang.monthNames[month0]
        val spoken = lang.speakDate(day, month0, year, ordinal, monthName)
        val displayAnswer = formatDate(lang, day, month0, year)
        return Question(
            category = Category.DATE,
            label = lang.categoryLabels.getValue(Category.DATE),
            speakText = spoken,
            displayAnswer = displayAnswer,
            placeholder = lang.placeholders.getValue(Category.DATE),
            checkAnswer = { input -> parseDate(input, lang) == Triple(day, month0 + 1, year) },
        )
    }

    fun genPercent(lang: LanguagePack, @Suppress("UNUSED_PARAMETER") settings: PracticeSettings, random: Random = Random.Default): Question {
        val useDecimal = random.nextDouble() < 0.3
        val value: Double
        val spokenNumber: String
        if (useDecimal) {
            val raw = randInt(0, 1000, random) / 10.0
            value = raw.roundTo(1)
            spokenNumber = value.formatFixed(1).replace('.', lang.decimalSeparator)
        } else {
            value = randInt(0, 100, random).toDouble()
            spokenNumber = value.toInt().toString()
        }
        val spoken = if (lang.percentHasSpace) "$spokenNumber %" else "$spokenNumber%"
        return Question(
            category = Category.PERCENT,
            label = lang.categoryLabels.getValue(Category.PERCENT),
            speakText = spoken,
            displayAnswer = spoken,
            placeholder = lang.placeholders.getValue(Category.PERCENT),
            checkAnswer = { input ->
                val n = normalizeNumberInput(input, lang).toDoubleOrNull()
                n != null && abs(n - value) < 1e-9
            },
        )
    }

    fun genPhone(lang: LanguagePack, random: Random = Random.Default): Question {
        val phone = lang.phoneNumber(random)
        return Question(
            category = Category.PHONE,
            label = lang.categoryLabels.getValue(Category.PHONE),
            speakText = phone.display,
            displayAnswer = phone.display,
            placeholder = lang.placeholders.getValue(Category.PHONE),
            checkAnswer = { input -> input.replace(Regex("\\D"), "") == phone.digits },
        )
    }

    fun genTime(lang: LanguagePack, @Suppress("UNUSED_PARAMETER") settings: PracticeSettings, random: Random = Random.Default): Question {
        val hour = randInt(0, 23, random)
        val minuteOptions = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
        val minute = minuteOptions[random.nextInt(minuteOptions.size)]
        val spoken = lang.speakTime(hour, minute)
        val displayAnswer = "${pad(hour)}:${pad(minute)}"
        return Question(
            category = Category.TIME,
            label = lang.categoryLabels.getValue(Category.TIME),
            speakText = spoken,
            displayAnswer = displayAnswer,
            placeholder = lang.placeholders.getValue(Category.TIME),
            checkAnswer = { input -> parseTime(input) == hour to minute },
        )
    }
}

/** Formats an amount in the locale's currency style, e.g. "12,50 €" or "$12.50"; [dropZeroCents] gives "20 €". */
internal fun formatMoney(cents: Int, lang: LanguagePack, dropZeroCents: Boolean = false): String {
    val amount = if (dropZeroCents && cents % 100 == 0) "${cents / 100}" else "${cents / 100}${lang.decimalSeparator}${pad(cents % 100)}"
    return if (lang.currencyBeforeValue) "${lang.currencySymbol}$amount" else "$amount ${lang.currencySymbol}"
}

/** A date in the locale's numeric order: "14/03/2027" (DMY) or "03/14/2027" (MDY). */
internal fun formatDate(lang: LanguagePack, day: Int, month0: Int, year: Int): String = when (lang.dateOrder) {
    DateOrder.DMY -> "${pad(day)}/${pad(month0 + 1)}/$year"
    DateOrder.MDY -> "${pad(month0 + 1)}/${pad(day)}/$year"
}

/** Parses a typed date in the locale's order into (day, month 1-12, year); 2-digit years map to 1950–2049. */
internal fun parseDate(input: String, lang: LanguagePack): Triple<Int, Int, Int>? {
    val cleaned = input.trim().replace(Regex("[.\\-\\s]"), "/")
    val match = Regex("^(\\d{1,2})/(\\d{1,2})/(\\d{2,4})$").find(cleaned) ?: return null
    val (a, b, yRaw) = match.destructured
    val (d, m) = when (lang.dateOrder) {
        DateOrder.DMY -> a.toInt() to b.toInt()
        DateOrder.MDY -> b.toInt() to a.toInt()
    }
    var y = yRaw.toInt()
    if (y < 100) y += if (y < 50) 2000 else 1900
    return Triple(d, m, y)
}

/** Parses a typed time like "14:35", "14h35", "1435" or "14" into (hour, minute), or null. */
internal fun parseTime(input: String): Pair<Int, Int>? {
    val cleaned = input.trim().lowercase().replace("h", ":").replace(Regex("\\s+"), "")
    val match = Regex("^(\\d{1,2}):?(\\d{0,2})$").find(cleaned) ?: return null
    val h = match.groupValues[1].toInt()
    val m = if (match.groupValues[2].isNotEmpty()) match.groupValues[2].toInt() else 0
    return h to m
}

private fun daysInMonth(year: Int, month0: Int): Int {
    val lengths = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val isLeap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    return if (month0 == 1 && isLeap) 29 else lengths[month0]
}

private fun Double.roundTo(places: Int): Double {
    val scale = 10.0.pow(places)
    return Math.round(this * scale) / scale
}

private fun Double.formatFixed(places: Int): String = String.format(java.util.Locale.ROOT, "%.${places}f", this)
