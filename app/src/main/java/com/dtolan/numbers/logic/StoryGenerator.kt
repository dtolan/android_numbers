package com.dtolan.numbers.logic

import com.dtolan.numbers.data.Category
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.PhoneNumber
import com.dtolan.numbers.data.StoryItem
import com.dtolan.numbers.data.StoryKey
import com.dtolan.numbers.data.StoryKey.*
import com.dtolan.numbers.data.StoryTheme
import java.time.LocalDate
import kotlin.math.abs
import kotlin.random.Random

/** Who says a line of a story; dialogue lines are voiced at a different pitch from the narrator. */
enum class Speaker { NARRATOR, YOU, OTHER }

data class StoryLine(val speaker: Speaker, val text: String)

/**
 * One spoken word problem: the [lines] to speak in order (narration and dialogue, ending with the
 * question) and the [question] that grades the typed answer. [Question.speakText] holds the
 * lines joined, for the transcript.
 */
data class Story(val lines: List<StoryLine>, val question: Question)

/**
 * Builds spoken word problems ("stories") from a language's [com.dtolan.numbers.data.StoryPack].
 * Each [StoryTheme] has its own ladder of levels, escalating from simple recall to arithmetic,
 * corrections ("6… no, 8"), distractor numbers and two-voice dialogues.
 */
object StoryGenerator {

    private val NOTES_CENTS = listOf(500, 1000, 2000, 5000, 10000)

    private val LEVELS: Map<StoryTheme, List<StoryBuilder.() -> Story>> = mapOf(
        StoryTheme.SHOPPING to listOf(
            { shoppingRecall(itemCount = 2, maxQty = 12) },
            { shoppingRecall(itemCount = r.nextInt(3, 5), maxQty = 30) },
            { shoppingTotal() },
            { shoppingPrices() },
            { shoppingDeli() },
            { shoppingChange() },
            { shoppingCorrection() },
            { shoppingFullTrip() },
            { shoppingDialogue() },
        ),
        StoryTheme.CAFE to listOf(
            { cafeOrder() }, { cafeBill() }, { cafeSplit() }, { cafeTable() }, { cafePaying() }, { cafeChangeOfMind() },
        ),
        StoryTheme.TRAVEL to listOf(
            { travelBus() }, { travelDeparture() }, { travelArrival() }, { travelCoach() }, { travelDelay() }, { travelTicket() },
        ),
        StoryTheme.APPOINTMENTS to listOf(
            { apptTime() }, { apptDate() }, { apptFilm() }, { apptOpening() }, { apptPostponed() }, { apptAge() }, { apptMeeting() },
        ),
        StoryTheme.HEALTH to listOf(
            { healthDose() }, { healthCourse() }, { healthPharmacy() }, { healthClinic() }, { healthPharmacist() },
        ),
        StoryTheme.HOME to listOf(
            { homeAddress() }, { homeApartment() }, { homeCode() }, { homeMessage() }, { homeCodeCorrection() }, { homeInvite() },
        ),
    )

    fun levelCount(theme: StoryTheme): Int = LEVELS.getValue(theme).size

    fun generate(theme: StoryTheme, level: Int, lang: LanguagePack, random: Random = Random.Default): Story {
        val levels = LEVELS.getValue(theme)
        val index = level.coerceIn(0, levels.size - 1)
        return levels[index](StoryBuilder(lang, random, lang.stories.levelTitles.getValue(theme)[index]))
    }

    // ---- Shopping ----

    /** "You're going to the store to pick up 10 apples and 2 bananas. How many bananas do you need?" */
    private fun StoryBuilder.shoppingRecall(itemCount: Int, maxQty: Int): Story {
        val items = pick(s.groceries, itemCount)
        // Distinct quantities, so the answer can't be guessed without catching which item it belongs to.
        val qtys = distinct(itemCount, 2..maxQty)
        val asked = r.nextInt(itemCount)
        say(SHOP_LIST, "list" to quantities(items, qtys))
        say(SHOP_ASK_HOW_MANY, "howMany" to s.howMany(items[asked]))
        return int(qtys[asked])
    }

    private fun StoryBuilder.shoppingTotal(): Story {
        val count = r.nextInt(2, 4)
        val qtys = List(count) { r.nextInt(2, 16) }
        say(SHOP_LIST, "list" to quantities(pick(s.groceries, count), qtys))
        say(SHOP_ASK_TOTAL)
        return int(qtys.sum())
    }

    private fun StoryBuilder.shoppingPrices(): Story {
        val (list, total) = pricedLines(s.groceries, r.nextInt(1, 3), maxQty = 6, minPrice = 50, maxPrice = 500)
        say(SHOP_PURCHASE, "list" to list)
        say(ASK_PAY)
        return money(total)
    }

    /** "200 grams of ham at €18 a kilo" — weights are a classic deli-counter listening trap. */
    private fun StoryBuilder.shoppingDeli(): Story {
        val grams = listOf(100, 150, 200, 250, 300, 400, 500, 750).random(r)
        val perKilo = r.nextInt(8, 41)
        say(SHOP_DELI, "grams" to grams, "item" to s.deliItems.random(r), "price" to spokenMoney(perKilo * 100))
        say(ASK_PAY)
        return money(grams * perKilo / 10)
    }

    private fun StoryBuilder.shoppingChange(): Story {
        val total = r.nextInt(21, 1000) * 5
        val note = noteAbove(total)
        say(SHOP_COST, "total" to spokenMoney(total))
        say(PAY_WITH_NOTE, "note" to spokenMoney(note))
        say(ASK_CHANGE)
        return money(note - total)
    }

    /** "…10 eggs and 4 pears. Oh wait, make that 7 eggs. How many eggs?" — catch the final number. */
    private fun StoryBuilder.shoppingCorrection(): Story {
        val count = r.nextInt(2, 4)
        val items = pick(s.groceries, count)
        val qtys = distinct(count + 1, 2..15)
        val asked = r.nextInt(count)
        val corrected = qtys.last()
        say(SHOP_LIST, "list" to quantities(items, qtys.dropLast(1)))
        say(SHOP_CORRECTION, "qty" to corrected, "item" to items[asked].plural)
        say(SHOP_ASK_HOW_MANY, "howMany" to s.howMany(items[asked]))
        return int(corrected)
    }

    private fun StoryBuilder.shoppingFullTrip(): Story {
        val (list, total) = pricedLines(s.groceries, r.nextInt(2, 4), maxQty = 4, minPrice = 50, maxPrice = 400)
        val note = noteAbove(total)
        say(SHOP_PURCHASE, "list" to list)
        say(PAY_WITH_NOTE, "note" to spokenMoney(note))
        say(ASK_CHANGE)
        return money(note - total)
    }

    /** A two-voice market exchange with a bundle deal: "€0.80 each, or 3 for €2." */
    private fun StoryBuilder.shoppingDialogue(): Story {
        val item = s.groceries.random(r)
        val price = r.nextInt(3, 16) * 10
        val n = r.nextInt(3, 6)
        val bundle = n * price - r.nextInt(1, n + 1) * 10
        val take = if (r.nextBoolean()) n else n + 1
        you(SHOP_DIALOGUE_ASK, "theItem" to s.the(item))
        other(SHOP_DIALOGUE_PRICE, "price" to spokenMoney(price), "n" to n, "bundle" to spokenMoney(bundle))
        you(SHOP_DIALOGUE_TAKE, "n" to take)
        say(ASK_PAY)
        return money(if (take == n) bundle else bundle + price)
    }

    // ---- Café ----

    private fun StoryBuilder.cafeOrder(): Story {
        val count = r.nextInt(2, 4)
        val items = pick(s.cafeItems, count)
        val qtys = distinct(count, 2..8)
        val asked = r.nextInt(count)
        say(CAFE_ORDER, "list" to quantities(items, qtys))
        say(CAFE_ASK_HOW_MANY, "howMany" to s.howMany(items[asked]))
        return int(qtys[asked])
    }

    private fun StoryBuilder.cafeBill(): Story {
        val (list, total) = pricedLines(s.cafeItems, 2, maxQty = 4, minPrice = 150, maxPrice = 600)
        say(CAFE_BILL, "list" to list)
        say(CAFE_ASK_BILL)
        return money(total)
    }

    private fun StoryBuilder.cafeSplit(): Story {
        val n = r.nextInt(2, 7)
        val share = r.nextInt(10, 61) * 50
        say(CAFE_SPLIT, "total" to spokenMoney(n * share), "n" to n)
        say(CAFE_ASK_EACH)
        return money(share)
    }

    /** Table number, party size and quantities all in one breath — answer only what's asked. */
    private fun StoryBuilder.cafeTable(): Story {
        val table = r.nextInt(2, 41)
        val people = (2..8).filter { it != table }.random(r)
        val qtys = distinct(2, 2..6)
        say(CAFE_TABLE, "table" to table, "people" to people, "list" to quantities(pick(s.cafeItems, 2), qtys))
        return if (r.nextBoolean()) {
            say(CAFE_ASK_TABLE); int(table)
        } else {
            say(CAFE_ASK_PEOPLE); int(people)
        }
    }

    private fun StoryBuilder.cafePaying(): Story {
        val total = r.nextInt(30, 451) * 10
        val note = noteAbove(total)
        other(CAFE_WAITER_TOTAL, "total" to spokenMoney(total))
        you(CAFE_YOU_PAY, "note" to spokenMoney(note))
        say(CAFE_ASK_WAITER_CHANGE)
        return money(note - total)
    }

    private fun StoryBuilder.cafeChangeOfMind(): Story {
        val count = r.nextInt(2, 4)
        val items = pick(s.cafeItems, count)
        val qtys = distinct(count + 1, 2..8)
        val changed = r.nextInt(count)
        val newQty = qtys.last()
        val order = qtys.dropLast(1)
        say(CAFE_ORDER, "list" to quantities(items, order))
        say(CAFE_CHANGE_MIND, "newQty" to newQty, "item" to items[changed].plural, "oldQty" to order[changed])
        say(CAFE_ASK_TOTAL_ITEMS)
        return int(order.sum() - order[changed] + newQty)
    }

    // ---- Travel ----

    /** Half the time from 70–99, the range French (soixante-dix, quatre-vingts…) makes hardest. */
    private fun StoryBuilder.trickyNumber(): Int = if (r.nextBoolean()) r.nextInt(70, 100) else r.nextInt(2, 70)

    private fun StoryBuilder.travelBus(): Story {
        val bus = trickyNumber()
        say(TRAVEL_BUS, "bus" to bus)
        say(TRAVEL_ASK_BUS)
        return int(bus)
    }

    private fun StoryBuilder.travelDeparture(): Story {
        val (h, m) = randomTime(6..22)
        say(TRAVEL_DEPARTS, "city" to s.cities.random(r), "time" to s.clock(h, m))
        say(TRAVEL_ASK_DEPARTURE)
        return time(h, m)
    }

    private fun StoryBuilder.travelArrival(): Story {
        val (h, m) = randomTime(6..21)
        val duration = r.nextInt(3, 36) * 5
        say(TRAVEL_DEPARTS, "city" to s.cities.random(r), "time" to s.clock(h, m))
        say(TRAVEL_DURATION, "duration" to s.duration(duration))
        say(TRAVEL_ASK_ARRIVAL)
        return timePlus(h, m, duration)
    }

    private fun StoryBuilder.travelCoach(): Story {
        val coach = trickyNumber()
        val bay = (1..20).filter { it != coach }.random(r)
        val (h, m) = randomTime(6..22)
        say(TRAVEL_COACH, "coach" to coach, "bay" to bay, "time" to s.clock(h, m))
        return when (r.nextInt(3)) {
            0 -> { say(TRAVEL_ASK_COACH); int(coach) }
            1 -> { say(TRAVEL_ASK_BAY); int(bay) }
            else -> { say(TRAVEL_ASK_COACH_TIME); time(h, m) }
        }
    }

    private fun StoryBuilder.travelDelay(): Story {
        val (h, m) = randomTime(6..22)
        val delay = r.nextInt(1, 12) * 5
        say(TRAVEL_DELAY, "time" to s.clock(h, m), "delay" to delay)
        say(TRAVEL_ASK_NEW_DEPARTURE)
        return timePlus(h, m, delay)
    }

    private fun StoryBuilder.travelTicket(): Story {
        val n = r.nextInt(2, 6)
        val price = r.nextInt(150, 891) * 10
        val platform = r.nextInt(1, 25)
        val (h, m) = randomTime(6..22)
        you(TRAVEL_TICKET_YOU, "n" to n, "city" to s.cities.random(r))
        other(TRAVEL_TICKET_CLERK, "price" to spokenMoney(price), "platform" to platform, "time" to s.clock(h, m))
        return when (r.nextInt(3)) {
            0 -> { say(ASK_PAY); money(n * price) }
            1 -> { say(TRAVEL_ASK_PLATFORM); int(platform) }
            else -> { say(TRAVEL_ASK_DEPARTURE); time(h, m) }
        }
    }

    // ---- Appointments ----

    private fun StoryBuilder.apptTime(): Story {
        val (h, m) = randomTime(8..18)
        say(APPT_TIME, "time" to s.clock(h, m))
        say(APPT_ASK_TIME)
        return time(h, m)
    }

    private fun StoryBuilder.apptDate(): Story {
        val day = randomDate()
        say(APPT_DATE, "date" to spokenDate(day))
        say(APPT_ASK_DATE)
        return date(day)
    }

    private fun StoryBuilder.apptFilm(): Story {
        val (h, m) = randomTime(14..21)
        val duration = r.nextInt(16, 36) * 5
        say(APPT_FILM, "time" to s.clock(h, m), "duration" to s.duration(duration))
        say(APPT_ASK_FILM_END)
        return timePlus(h, m, duration)
    }

    private fun StoryBuilder.apptOpening(): Story {
        val a = r.nextInt(8, 11)
        val b = a + r.nextInt(3, 5)
        val c = b + r.nextInt(1, 3)
        val d = c + r.nextInt(3, 6)
        say(APPT_OPENING, "a" to s.clock(a, 0), "b" to s.clock(b, 0), "c" to s.clock(c, 0), "d" to s.clock(d, 0))
        say(APPT_ASK_OPEN_HOURS)
        return int((b - a) + (d - c))
    }

    private fun StoryBuilder.apptPostponed(): Story {
        val day = randomDate()
        val days = r.nextInt(2, 21)
        say(APPT_POSTPONED, "date" to spokenDate(day), "days" to days)
        say(APPT_ASK_NEW_DATE)
        return date(day.plusDays(days.toLong()))
    }

    /** Ages 70–99: the hardest stretch of French numbers, plus hearing a full year. */
    private fun StoryBuilder.apptAge(): Story {
        val now = r.nextInt(2025, 2031)
        val age = r.nextInt(70, 100)
        say(APPT_AGE, "year" to now - age)
        say(APPT_ASK_AGE, "now" to now)
        return int(age)
    }

    private fun StoryBuilder.apptMeeting(): Story {
        val (h1, m1) = randomTime(8..17)
        val shifted = h1 * 60 + m1 + (if (r.nextBoolean()) 1 else -1) * r.nextInt(3, 25) * 5
        val (h2, m2) = shifted / 60 to shifted % 60
        say(APPT_MEETING, "t1" to s.clock(h1, m1), "t2" to s.clock(h2, m2))
        say(APPT_ASK_MEETING)
        return time(h2, m2)
    }

    // ---- Health ----

    private fun StoryBuilder.healthDose(): Story {
        val (perDose, times) = distinct(2, 2..4)
        say(HEALTH_DOSE, "perDose" to perDose, "times" to times)
        say(HEALTH_ASK_PER_DOSE)
        return int(perDose)
    }

    private fun StoryBuilder.healthCourse(): Story {
        val perDose = r.nextInt(2, 4)
        val times = r.nextInt(2, 5)
        val days = r.nextInt(3, 15)
        say(HEALTH_COURSE, "perDose" to perDose, "times" to times, "days" to days)
        say(HEALTH_ASK_TOTAL)
        return int(perDose * times * days)
    }

    private fun StoryBuilder.healthPharmacy(): Story {
        val n = r.nextInt(2, 5)
        val price = r.nextInt(50, 301) * 5
        val note = noteAbove(n * price)
        say(HEALTH_PHARMACY, "n" to n, "price" to spokenMoney(price))
        say(PAY_WITH_NOTE, "note" to spokenMoney(note))
        say(ASK_CHANGE)
        return money(note - n * price)
    }

    private fun StoryBuilder.healthClinic(): Story {
        val floor = r.nextInt(2, 10)
        val room = r.nextInt(10, 61)
        val (h, m) = randomTime(8..18)
        say(HEALTH_CLINIC, "floor" to floor, "room" to room, "time" to s.clock(h, m))
        return when (r.nextInt(3)) {
            0 -> { say(ASK_FLOOR); int(floor) }
            1 -> { say(HEALTH_ASK_ROOM); int(room) }
            else -> { say(APPT_ASK_TIME); time(h, m) }
        }
    }

    /** Pharmacist dialogue: tablets per day × days, divided into boxes — rounding up. */
    private fun StoryBuilder.healthPharmacist(): Story {
        val perDose = r.nextInt(2, 4)
        val days = r.nextInt(5, 15)
        val box = listOf(10, 12, 14, 16, 20, 24, 28, 30).random(r)
        other(HEALTH_PHARMACIST, "perDose" to perDose, "days" to days)
        you(HEALTH_YOU_BOXES)
        other(HEALTH_BOX_SIZE, "box" to box)
        say(HEALTH_ASK_BOXES)
        val tablets = perDose * 2 * days
        return int((tablets + box - 1) / box)
    }

    // ---- Home ----

    private fun StoryBuilder.homeAddress(): Story {
        val number = r.nextInt(2, 130)
        say(HOME_ADDRESS, "number" to number, "street" to s.streets.random(r))
        say(HOME_ASK_NUMBER)
        return int(number)
    }

    private fun StoryBuilder.homeApartment(): Story {
        val number = r.nextInt(10, 130)
        val floor = r.nextInt(2, 10)
        val apt = (2..60).filter { it != number && it != floor }.random(r)
        say(HOME_FULL, "number" to number, "street" to s.streets.random(r), "floor" to floor, "apt" to apt)
        return when (r.nextInt(3)) {
            0 -> { say(HOME_ASK_NUMBER); int(number) }
            1 -> { say(ASK_FLOOR); int(floor) }
            else -> { say(HOME_ASK_APT); int(apt) }
        }
    }

    private fun StoryBuilder.homeCode(): Story {
        val code = randomCode()
        say(HOME_CODE, "code" to spokenDigits(code))
        say(HOME_ASK_CODE)
        return digits(code)
    }

    private fun StoryBuilder.homeMessage(): Story {
        val home = lang.phoneNumber(r)
        val office = lang.phoneNumber(r)
        say(HOME_MESSAGE, "home" to home.display, "office" to office.display)
        say(HOME_ASK_HOME_PHONE)
        return phone(home)
    }

    /** "The code is 4821. Oh no, sorry, it's 4812." — a realistic swapped-digit correction. */
    private fun StoryBuilder.homeCodeCorrection(): Story {
        // Distinct digits guarantee the swap changes the code; swapping positions 1–3 keeps the first digit non-zero.
        val lead = r.nextInt(1, 10)
        val first = (listOf(lead) + (0..9).filter { it != lead }.shuffled(r).take(3)).joinToString("")
        val chars = first.toCharArray()
        val i = r.nextInt(1, 3)
        chars[i] = chars[i + 1].also { chars[i + 1] = chars[i] }
        val second = String(chars)
        say(HOME_CODE_CORRECTION, "c1" to spokenDigits(first), "c2" to spokenDigits(second))
        say(HOME_ASK_CODE)
        return digits(second)
    }

    private fun StoryBuilder.homeInvite(): Story {
        val (h, m) = randomTime(17..21)
        val number = r.nextInt(10, 130)
        val floor = r.nextInt(2, 10)
        val code = randomCode()
        other(
            HOME_INVITE,
            "time" to s.clock(h, m), "number" to number, "street" to s.streets.random(r),
            "floor" to floor, "code" to spokenDigits(code),
        )
        return when (r.nextInt(4)) {
            0 -> { say(HOME_ASK_FRIEND_NUMBER); int(number) }
            1 -> { say(ASK_FLOOR); int(floor) }
            2 -> { say(HOME_ASK_CODE); digits(code) }
            else -> { say(HOME_ASK_PARTY_TIME); time(h, m) }
        }
    }

    private fun noteAbove(cents: Int) = NOTES_CENTS.first { it > cents }
}

/** Accumulates a story's lines and turns the final answer into a graded [Story]. */
private class StoryBuilder(val lang: LanguagePack, val r: Random, private val label: String) {
    val s = lang.stories
    private val lines = mutableListOf<StoryLine>()

    private fun fill(key: StoryKey, args: Array<out Pair<String, Any>>): String =
        args.fold(s.templates.getValue(key)) { text, (name, value) -> text.replace("{$name}", value.toString()) }

    fun say(key: StoryKey, vararg args: Pair<String, Any>) { lines += StoryLine(Speaker.NARRATOR, fill(key, args)) }
    fun you(key: StoryKey, vararg args: Pair<String, Any>) { lines += StoryLine(Speaker.YOU, fill(key, args)) }
    fun other(key: StoryKey, vararg args: Pair<String, Any>) { lines += StoryLine(Speaker.OTHER, fill(key, args)) }

    fun <T> pick(from: List<T>, count: Int): List<T> = from.shuffled(r).take(count)
    fun distinct(count: Int, range: IntRange): List<Int> = range.shuffled(r).take(count)
    fun quantities(items: List<StoryItem>, qtys: List<Int>): String =
        s.join(items.zip(qtys) { item, qty -> "$qty ${item.plural}" })

    /** "N items at €X each" lines; returns the spoken list and the total in cents. */
    fun pricedLines(from: List<StoryItem>, count: Int, maxQty: Int, minPrice: Int, maxPrice: Int): Pair<String, Int> {
        var total = 0
        val lines = pick(from, count).map { item ->
            val qty = r.nextInt(2, maxQty + 1)
            val price = r.nextInt(minPrice / 10, maxPrice / 10 + 1) * 10
            total += qty * price
            fill(StoryKey.PRICED_LINE, arrayOf("qty" to qty, "item" to item.plural, "price" to spokenMoney(price)))
        }
        return s.join(lines) to total
    }

    fun spokenMoney(cents: Int) = formatMoney(cents, lang, dropZeroCents = true)
    fun randomTime(hours: IntRange): Pair<Int, Int> = hours.random(r) to r.nextInt(12) * 5
    fun randomDate(): LocalDate = LocalDate.of(r.nextInt(2026, 2029), 1, 1).plusDays(r.nextLong(365))
    fun spokenDate(d: LocalDate): String {
        val month0 = d.monthValue - 1
        return lang.speakDate(d.dayOfMonth, month0, d.year, lang.ordinalDay(d.dayOfMonth), lang.monthNames[month0])
    }
    fun randomCode(): String = "${r.nextInt(1, 10)}" + List(3) { r.nextInt(10) }.joinToString("")
    fun spokenDigits(code: String) = code.toList().joinToString(" ")

    private fun build(category: Category, display: String, check: (String) -> Boolean) = Story(
        lines = lines.toList(),
        question = Question(
            category = category,
            label = label,
            speakText = lines.joinToString(" ") { it.text },
            displayAnswer = display,
            placeholder = lang.placeholders.getValue(category),
            checkAnswer = check,
        ),
    )

    fun int(value: Int) = build(Category.INTEGER, value.toString()) { normalizeNumberInput(it, lang).toIntOrNull() == value }

    fun money(cents: Int) = build(Category.MONEY, formatMoney(cents, lang)) { input ->
        val n = normalizeNumberInput(input, lang).toDoubleOrNull()
        n != null && abs(n * 100 - cents) < 0.5
    }

    fun time(h: Int, m: Int) = build(Category.TIME, "${pad(h)}:${pad(m)}") { parseTime(it) == h to m }

    fun timePlus(h: Int, m: Int, minutes: Int): Story {
        val total = (h * 60 + m + minutes) % (24 * 60)
        return time(total / 60, total % 60)
    }

    fun date(d: LocalDate) = build(Category.DATE, formatDate(lang, d.dayOfMonth, d.monthValue - 1, d.year)) {
        parseDate(it, lang) == Triple(d.dayOfMonth, d.monthValue, d.year)
    }

    fun digits(code: String) = build(Category.INTEGER, code) { it.replace(Regex("\\D"), "") == code }

    fun phone(p: PhoneNumber) = build(Category.PHONE, p.display) { it.replace(Regex("\\D"), "") == p.digits }
}
