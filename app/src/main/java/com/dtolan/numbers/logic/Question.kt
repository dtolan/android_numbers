package com.dtolan.numbers.logic

import com.dtolan.numbers.data.Category

/** One generated quiz item: what to speak, what the correct answer looks like, how to grade it. */
data class Question(
    val category: Category,
    val label: String,
    val speakText: String,
    val displayAnswer: String,
    val placeholder: String,
    val checkAnswer: (String) -> Boolean,
    /** Set for flash-drill (Learn mode) questions: tappable options, one of which is [displayAnswer]. */
    val choices: List<String>? = null,
)

/** User-adjustable ranges shared by the number generators. */
data class PracticeSettings(
    val minVal: Int = 0,
    val maxVal: Int = 100,
    val decimalPlaces: Int = 2,
    val yearMin: Int = 1950,
    val yearMax: Int = 2026,
    val speechRate: Float = 0.9f,
    val enabledCategories: Set<Category> = setOf(Category.INTEGER),
)
