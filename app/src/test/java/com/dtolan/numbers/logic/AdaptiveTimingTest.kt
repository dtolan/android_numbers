package com.dtolan.numbers.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveTimingTest {

    @Test
    fun `shrink multiplies by the decay factor`() {
        assertEquals(5100L, AdaptiveTiming.shrink(6000, 1500, 0.85))
    }

    @Test
    fun `shrink never goes below the floor`() {
        assertEquals(1500L, AdaptiveTiming.shrink(1600, 1500, 0.85))
        assertEquals(1500L, AdaptiveTiming.shrink(1500, 1500, 0.85))
    }

    @Test
    fun `repeated shrinking converges to the floor, not below it`() {
        var current = 6000L
        repeat(100) { current = AdaptiveTiming.shrink(current, 1500, 0.85) }
        assertTrue(current >= 1500L)
        assertEquals(1500L, current)
    }
}
