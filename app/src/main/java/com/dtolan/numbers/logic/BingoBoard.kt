package com.dtolan.numbers.logic

import kotlin.random.Random

/** Pure board-generation and win-check logic for Number Bingo — a 3x3 card. */
object BingoBoard {
    const val SIZE = 3

    /** Builds a SIZE x SIZE board of distinct values drawn from [pool] (repeating only if the pool itself has fewer than SIZE*SIZE distinct values). */
    fun generateBoard(pool: List<Int>, random: Random = Random.Default): List<Int> {
        val distinct = pool.distinct()
        val needed = SIZE * SIZE
        return if (distinct.size >= needed) {
            distinct.shuffled(random).take(needed)
        } else {
            val shuffled = distinct.shuffled(random)
            (0 until needed).map { shuffled[it % shuffled.size] }
        }
    }

    /** True if any full row, column or diagonal of the board is contained in [marked]. */
    fun hasWon(board: List<Int>, marked: Set<Int>): Boolean {
        require(board.size == SIZE * SIZE) { "board must have ${SIZE * SIZE} cells" }
        fun rowWin(r: Int) = (0 until SIZE).all { c -> board[r * SIZE + c] in marked }
        fun colWin(c: Int) = (0 until SIZE).all { r -> board[r * SIZE + c] in marked }
        val diag1 = (0 until SIZE).all { i -> board[i * SIZE + i] in marked }
        val diag2 = (0 until SIZE).all { i -> board[i * SIZE + (SIZE - 1 - i)] in marked }
        return (0 until SIZE).any(::rowWin) || (0 until SIZE).any(::colWin) || diag1 || diag2
    }
}
