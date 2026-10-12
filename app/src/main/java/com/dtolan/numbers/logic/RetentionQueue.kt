package com.dtolan.numbers.logic

import kotlin.random.Random

/**
 * Graduated-interval recall scheduler — the algorithm behind Pimsleur-style passive audio
 * courses (listen while driving/running, no screen). A freshly-introduced value comes back soon
 * (after [intervalSchedule].first() other items), then at growing gaps, so it's still "warm" the
 * first few times but eventually only resurfaces occasionally — while new values keep entering
 * the stream. This is spaced repetition compressed into a single continuous session rather than
 * spread across days, which is the right shape for a single workout's worth of listening.
 *
 * [source] supplies a fresh candidate value on demand (e.g. a random draw from a Learn stage's
 * range) — the queue itself has no notion of what a "value" means, just when to repeat it.
 */
class RetentionQueue(
    private val source: () -> Int,
    private val random: Random = Random.Default,
    private val initialRepeats: Int = 4,
    private val intervalSchedule: List<Int> = listOf(2, 4, 8, 16),
) : NumberSource {
    private data class Item(val value: Int, var repeatsRemaining: Int, var dueInSteps: Int, var exposureCount: Int = 0)

    private val active = mutableListOf<Item>()
    private var lastValue: Int? = null

    /** Advances the schedule by one step and returns the next value to speak. */
    override fun next(): NumberPick {
        for (item in active) item.dueInSteps--

        val due = active.filter { it.dueInSteps <= 0 }
        val pick: NumberPick
        if (due.isNotEmpty()) {
            val item = due[random.nextInt(due.size)]
            item.repeatsRemaining--
            item.exposureCount++
            if (item.repeatsRemaining <= 0) {
                active.remove(item)
            } else {
                val stepIndex = (intervalSchedule.size - item.repeatsRemaining).coerceIn(0, intervalSchedule.lastIndex)
                item.dueInSteps = intervalSchedule[stepIndex]
            }
            pick = NumberPick(item.value, exposureNumber = item.exposureCount)
        } else {
            var candidate = source()
            var attempts = 0
            while (candidate == lastValue && attempts < 5) {
                candidate = source()
                attempts++
            }
            val item = Item(candidate, initialRepeats, intervalSchedule.first(), exposureCount = 1)
            active.add(item)
            pick = NumberPick(candidate, exposureNumber = item.exposureCount)
        }
        lastValue = pick.value
        return pick
    }
}
