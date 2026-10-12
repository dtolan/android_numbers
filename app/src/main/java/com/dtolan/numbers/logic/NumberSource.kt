package com.dtolan.numbers.logic

import kotlin.random.Random

/**
 * One value drawn from a [NumberSource] for the passive Listen drill. [exposureNumber] (1-based)
 * is how many times this value has been heard so far in the current session, including this pick
 * — used to decide how many times to play a "confirm in native language" echo before it fades out.
 */
data class NumberPick(val value: Int, val exposureNumber: Int) {
    val isFirstExposure: Boolean get() = exposureNumber == 1
}

/** Supplies the next value to speak in the passive Listen drill, in whatever order a given listening mode calls for. */
interface NumberSource {
    fun next(): NumberPick
}

/** Plays every value in [values] once, in order, then loops back to the start — good for systematically walking an irregular range like French's 70-99. */
class SequentialSource(private val values: List<Int>) : NumberSource {
    init { require(values.isNotEmpty()) { "SequentialSource needs at least one value" } }

    private var index = 0
    private var pass = 1

    override fun next(): NumberPick {
        val pick = NumberPick(values[index], pass)
        index++
        if (index >= values.size) {
            index = 0
            pass++
        }
        return pick
    }
}

/** Plays every value in [values] exactly once per pass, in a fresh random order each pass (a "shuffle bag") — never repeats a value back-to-back across the pass boundary. */
class ShuffleSource(private val values: List<Int>, private val random: Random = Random.Default) : NumberSource {
    init { require(values.isNotEmpty()) { "ShuffleSource needs at least one value" } }

    private var bag = mutableListOf<Int>()
    private var pass = 0
    private var lastValue: Int? = null

    override fun next(): NumberPick {
        if (bag.isEmpty()) {
            pass++
            bag = values.shuffled(random).toMutableList()
            if (bag.size > 1 && bag.first() == lastValue) {
                val swapIndex = 1 + random.nextInt(bag.size - 1)
                val tmp = bag[0]
                bag[0] = bag[swapIndex]
                bag[swapIndex] = tmp
            }
        }
        val value = bag.removeAt(0)
        lastValue = value
        return NumberPick(value, pass)
    }
}

/** Which way a Count session runs through its range. */
enum class CountDirection { UP, DOWN, UP_DOWN }

/**
 * The values a Count session speaks, in order: every integer between [from] and [to] inclusive
 * (in whichever order the two ends were given), ascending, descending, or up then back down.
 * For [CountDirection.UP_DOWN] with [loop], the bottom end is left off the tail so repeating the
 * list oscillates smoothly (1 2 3 2 | 1 2 3 2 …) without saying either end twice in a row.
 */
fun countValues(from: Int, to: Int, direction: CountDirection, loop: Boolean = false): List<Int> {
    val ascending = (minOf(from, to)..maxOf(from, to)).toList()
    return when (direction) {
        CountDirection.UP -> ascending
        CountDirection.DOWN -> ascending.reversed()
        CountDirection.UP_DOWN -> {
            val down = ascending.reversed().drop(1)
            ascending + if (loop) down.dropLast(1) else down
        }
    }
}
