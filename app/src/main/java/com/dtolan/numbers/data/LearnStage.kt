package com.dtolan.numbers.data

/**
 * One gated step in a language's guided "Learn" curriculum (see [LanguagePack.learnStages]).
 *
 * Stages are ordered and gated: stage N+1 unlocks once the learner answers [requiredStreak]
 * questions from stage N correctly in a row. This models blocked practice on the specific
 * sub-ranges that are pedagogically hard for a given language (e.g. French 70-99, which is built
 * by recombining numbers the learner already knows rather than being new vocabulary), instead of
 * drawing uniformly at random across the whole 0-100 range from the start.
 */
data class LearnStage(
    val title: String,
    val description: String,
    val min: Int,
    val max: Int,
    /** When set, questions are drawn only from these exact values instead of [min]..[max]. */
    val values: List<Int>? = null,
    val requiredStreak: Int = 8,
    /** Generous starting time limit (ms) for an answer to "count" toward the streak. */
    val startTimeLimitMs: Long = 6000,
    /** The fastest the time limit is allowed to shrink to — the fluency floor. */
    val minTimeLimitMs: Long = 1500,
    /** Multiplied into the current time limit after each qualifying (correct-and-fast) answer. */
    val timeDecayFactor: Double = 0.85,
)
