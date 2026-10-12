package com.dtolan.numbers.logic

/** Pure math behind the Learn-mode adaptive response-time gate. */
object AdaptiveTiming {
    /** The next (tighter) time limit after a qualifying answer, never shrinking past [min]. */
    fun shrink(current: Long, min: Long, factor: Double): Long =
        (current * factor).toLong().coerceAtLeast(min)
}
