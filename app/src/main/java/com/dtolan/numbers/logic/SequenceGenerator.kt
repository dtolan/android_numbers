package com.dtolan.numbers.logic

import kotlin.random.Random

/** A short run of numbers, the correct next value, and flash-drill style choices. */
data class NumberSequence(val values: List<Int>, val next: Int, val choices: List<String>)

/** Difficulty level for [SequenceGenerator.generate] — chosen by the player, not tied to a Learn stage. */
enum class SequenceLevel { BASIC, ADVANCED }

/**
 * "What comes next?" generator — hears a run of 2-4 numbers, must predict the next one. At
 * [SequenceLevel.BASIC] the run always counts by 1, in either direction (ascending or
 * descending), directly drilling the recombination rule behind irregular decades (French 70-99
 * etc.): you can't just recall an isolated number, you have to track where you are in the
 * counting sequence and know what continues it. At [SequenceLevel.ADVANCED] the run skip-counts
 * by a larger step (2, 3, 5 or 10), which forces recognizing the pattern rather than just "one
 * more than the last number heard".
 */
object SequenceGenerator {
    private val ADVANCED_STEPS = listOf(2, 3, 5, 10)

    fun generate(min: Int, max: Int, random: Random = Random.Default, level: SequenceLevel = SequenceLevel.BASIC, choiceCount: Int = 4): NumberSequence? {
        val span = max - min
        if (span < 2) return null

        val stepCandidates = if (level == SequenceLevel.ADVANCED) ADVANCED_STEPS.shuffled(random) else listOf(1)
        for (step in stepCandidates) {
            val maxLength = minOf(4, span / step)
            if (maxLength < 2) continue
            val length = random.nextInt(2, maxLength + 1)
            val descending = random.nextBoolean()
            val stepSigned = if (descending) -step else step
            val totalSpan = length * step

            val start = if (!descending) {
                val latestStart = max - totalSpan
                if (latestStart < min) continue
                random.nextInt(min, latestStart + 1)
            } else {
                val earliestStart = min + totalSpan
                if (earliestStart > max) continue
                random.nextInt(earliestStart, max + 1)
            }

            val values = (0 until length).map { start + it * stepSigned }
            val next = start + length * stepSigned
            val pool = (min..max).filter { it != next }
            val preferred = listOf(values.last(), next + stepSigned, next - 2 * stepSigned, start - stepSigned)
                .filter { it in pool }
            val distractorCount = (choiceCount - 1).coerceAtMost(pool.size)
            val distractors = (preferred.shuffled(random) + pool.shuffled(random)).distinct().take(distractorCount)
            val choices = (distractors + next).map { it.toString() }.shuffled(random)
            return NumberSequence(values, next, choices)
        }
        return null
    }
}
