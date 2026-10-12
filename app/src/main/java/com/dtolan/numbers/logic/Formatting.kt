package com.dtolan.numbers.logic

/** Zero-pads [n] to [len] digits, e.g. pad(7) == "07". */
fun pad(n: Int, len: Int = 2): String = n.toString().padStart(len, '0')

/** English ordinal suffix: 1 -> "1st", 2 -> "2nd", 3 -> "3rd", 11 -> "11th", 21 -> "21st", ... */
fun englishOrdinal(day: Int): String {
    val suffix = if (day % 100 in 11..13) {
        "th"
    } else {
        when (day % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
    }
    return "$day$suffix"
}
