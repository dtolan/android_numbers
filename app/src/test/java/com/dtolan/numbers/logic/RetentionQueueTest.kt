package com.dtolan.numbers.logic

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetentionQueueTest {

    @Test
    fun `a value appears once on introduction plus initialRepeats more times, then is dropped`() {
        var counter = 100
        val queue = RetentionQueue(source = { counter++ }, initialRepeats = 2, intervalSchedule = listOf(1))

        val sequence = List(3) { queue.next().value }
        assertEquals(listOf(100, 100, 100), sequence) // 1 intro + 2 repeats, interval 1 => due every step

        assertEquals(101, queue.next().value) // exhausted and removed -> a fresh value follows
    }

    @Test
    fun `only the introducing pick is flagged as first exposure, repeats are not`() {
        var counter = 100
        val queue = RetentionQueue(source = { counter++ }, initialRepeats = 2, intervalSchedule = listOf(1))

        val intro = queue.next()
        assertTrue(intro.isFirstExposure)
        val repeat1 = queue.next()
        assertFalse(repeat1.isFirstExposure)
        val repeat2 = queue.next()
        assertFalse(repeat2.isFirstExposure)

        val nextIntro = queue.next()
        assertTrue(nextIntro.isFirstExposure) // a fresh value after the previous one was exhausted
    }

    @Test
    fun `exposureNumber counts up across introduction and each repeat`() {
        var counter = 100
        val queue = RetentionQueue(source = { counter++ }, initialRepeats = 2, intervalSchedule = listOf(1))

        assertEquals(1, queue.next().exposureNumber) // introduction
        assertEquals(2, queue.next().exposureNumber) // 1st repeat
        assertEquals(3, queue.next().exposureNumber) // 2nd repeat, exhausts and removes the item
        assertEquals(1, queue.next().exposureNumber) // exhausted -> fresh value starts back at 1
    }

    @Test
    fun `default schedule never repeats a value on the very next step`() {
        var counter = 0
        val queue = RetentionQueue(source = { counter++ }, random = Random(1))
        var previous = queue.next().value
        repeat(200) {
            val value = queue.next().value
            assertTrue("value $value repeated immediately after $previous", value != previous)
            previous = value
        }
    }

    @Test
    fun `keeps introducing new values over a long session instead of looping forever`() {
        var counter = 0
        val queue = RetentionQueue(source = { counter++ }, random = Random(7))
        val seen = mutableSetOf<Int>()
        repeat(100) { seen.add(queue.next().value) }
        assertTrue("expected multiple distinct values to have been introduced, got $seen", seen.size >= 10)
    }

    @Test
    fun `restricts to whatever the source range produces`() {
        val queue = RetentionQueue(source = { Random.nextInt(70, 80) }, random = Random(3))
        repeat(100) {
            val value = queue.next().value
            assertTrue(value in 70 until 80)
        }
    }
}
