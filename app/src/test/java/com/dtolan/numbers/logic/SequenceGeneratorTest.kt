package com.dtolan.numbers.logic

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SequenceGeneratorTest {

    @Test
    fun `basic values step by 1 in a consistent direction and next continues them`() {
        repeat(50) { seed ->
            val seq = SequenceGenerator.generate(70, 79, Random(seed)) ?: return@repeat
            val step = seq.values[1] - seq.values[0]
            assertTrue(step == 1 || step == -1)
            for (i in 1 until seq.values.size) {
                assertEquals(step, seq.values[i] - seq.values[i - 1])
            }
            assertEquals(seq.values.last() + step, seq.next)
        }
    }

    @Test
    fun `advanced values skip-count by a consistent step and next continues them`() {
        repeat(50) { seed ->
            val seq = SequenceGenerator.generate(0, 100, Random(seed), level = SequenceLevel.ADVANCED) ?: return@repeat
            val step = seq.values[1] - seq.values[0]
            assertTrue(kotlin.math.abs(step) in listOf(2, 3, 5, 10))
            for (i in 1 until seq.values.size) {
                assertEquals(step, seq.values[i] - seq.values[i - 1])
            }
            assertEquals(seq.values.last() + step, seq.next)
        }
    }

    @Test
    fun `choices always include the correct next value exactly once`() {
        repeat(50) { seed ->
            val seq = SequenceGenerator.generate(0, 20, Random(seed)) ?: return@repeat
            assertEquals(1, seq.choices.count { it == seq.next.toString() })
        }
    }

    @Test
    fun `stays within the requested range`() {
        repeat(50) { seed ->
            val seq = SequenceGenerator.generate(70, 79, Random(seed)) ?: return@repeat
            assertTrue(seq.values.all { it in 70..79 })
            assertTrue(seq.next in 70..79)
        }
    }

    @Test
    fun `returns null for a range too small to hold any run`() {
        assertNull(SequenceGenerator.generate(5, 6, Random(1)))
    }

    @Test
    fun `handles a range just barely large enough`() {
        assertNotNull(SequenceGenerator.generate(0, 3, Random(1)))
    }
}
