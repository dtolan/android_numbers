package com.dtolan.numbers.logic

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SequentialSourceTest {

    @Test
    fun `plays every value in order, then loops back to the start`() {
        val source = SequentialSource(listOf(10, 11, 12))
        val values = List(7) { source.next().value }
        assertEquals(listOf(10, 11, 12, 10, 11, 12, 10), values)
    }

    @Test
    fun `exposureNumber is the pass count, same for every value in a pass`() {
        val source = SequentialSource(listOf(1, 2))
        val picks = List(6) { source.next() }
        assertEquals(listOf(1, 1, 2, 2, 3, 3), picks.map { it.exposureNumber })
    }
}

class ShuffleSourceTest {

    @Test
    fun `each pass contains every value exactly once`() {
        val values = (0..9).toList()
        val source = ShuffleSource(values, Random(1))
        val firstPass = List(10) { source.next().value }
        assertEquals(values.toSet(), firstPass.toSet())
    }

    @Test
    fun `never repeats a value on the very next draw, even across a pass boundary`() {
        val source = ShuffleSource((0..5).toList(), Random(7))
        var previous = source.next().value
        repeat(200) {
            val value = source.next().value
            assertTrue("value $value repeated immediately after $previous", value != previous)
            previous = value
        }
    }

    @Test
    fun `exposureNumber increments once per full pass`() {
        val source = ShuffleSource(listOf(1, 2, 3), Random(3))
        val firstPassExposures = List(3) { source.next().exposureNumber }
        val secondPassExposures = List(3) { source.next().exposureNumber }
        assertEquals(listOf(1, 1, 1), firstPassExposures)
        assertEquals(listOf(2, 2, 2), secondPassExposures)
    }

    @Test
    fun `count values run low to high, high to low, and accept the ends in either order`() {
        assertEquals(listOf(3, 4, 5, 6), countValues(3, 6, CountDirection.UP))
        assertEquals(listOf(6, 5, 4, 3), countValues(3, 6, CountDirection.DOWN))
        assertEquals(listOf(3, 4, 5, 6), countValues(6, 3, CountDirection.UP))
        assertEquals(listOf(70), countValues(70, 70, CountDirection.DOWN))
        // Up and down once ends back at the start; looped, the bottom isn't repeated at the seam.
        assertEquals(listOf(3, 4, 5, 6, 5, 4, 3), countValues(3, 6, CountDirection.UP_DOWN))
        assertEquals(listOf(3, 4, 5, 6, 5, 4), countValues(3, 6, CountDirection.UP_DOWN, loop = true))
        assertEquals(listOf(3, 4), countValues(3, 4, CountDirection.UP_DOWN, loop = true))
        assertEquals(listOf(70), countValues(70, 70, CountDirection.UP_DOWN, loop = true))
    }
}
