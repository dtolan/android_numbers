package com.dtolan.numbers.logic

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BingoBoardTest {

    @Test
    fun `board has 9 cells`() {
        val board = BingoBoard.generateBoard((0..20).toList(), Random(1))
        assertEquals(9, board.size)
    }

    @Test
    fun `board cells are distinct when the pool is large enough`() {
        val board = BingoBoard.generateBoard((0..20).toList(), Random(1))
        assertEquals(9, board.toSet().size)
    }

    @Test
    fun `board pads with repeats when the pool is smaller than 9`() {
        val pool = listOf(10, 20, 30, 40, 50)
        val board = BingoBoard.generateBoard(pool, Random(1))
        assertEquals(9, board.size)
        assertTrue(board.all { it in pool })
    }

    @Test
    fun `no win with nothing marked`() {
        val board = (1..9).toList()
        assertFalse(BingoBoard.hasWon(board, emptySet()))
    }

    @Test
    fun `win on a full row`() {
        val board = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
        assertTrue(BingoBoard.hasWon(board, setOf(4, 5, 6))) // middle row
    }

    @Test
    fun `win on a full column`() {
        val board = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
        assertTrue(BingoBoard.hasWon(board, setOf(1, 4, 7))) // left column
    }

    @Test
    fun `win on the main diagonal`() {
        val board = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
        assertTrue(BingoBoard.hasWon(board, setOf(1, 5, 9)))
    }

    @Test
    fun `win on the anti-diagonal`() {
        val board = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
        assertTrue(BingoBoard.hasWon(board, setOf(3, 5, 7)))
    }

    @Test
    fun `partial row is not a win`() {
        val board = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
        assertFalse(BingoBoard.hasWon(board, setOf(1, 2)))
    }
}
