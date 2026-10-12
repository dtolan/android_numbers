package com.dtolan.numbers.logic

import com.dtolan.numbers.data.StoryPack

/**
 * A counted item inside a story line — "3 pommes" — with where it sits in [lineIndex]'s text
 * ([start] is the number, so the cue fires as the voice says it) and the item's picture.
 */
data class PictureCue(val lineIndex: Int, val start: Int, val count: Int, val word: String, val picture: String)

/**
 * Every "<number> <pictured item>" in [story]'s lines, in speaking order, so the screen can show
 * 🍎🍎🍎 as the voice says "tre mele". Items without a count right before them ("Combien de
 * pommes ?", "400 grammes de jambon") get no cue.
 */
fun pictureCues(story: Story, pack: StoryPack): List<PictureCue> =
    story.lines.flatMapIndexed { lineIndex, line ->
        pack.pictures.flatMap { (word, picture) ->
            // Whole words only: "pere" (pears) must not match inside another word.
            Regex("(?<![\\p{L}\\d,.])(\\d+)\\s+${Regex.escape(word)}(?!\\p{L})", RegexOption.IGNORE_CASE)
                .findAll(line.text)
                .map { PictureCue(lineIndex, it.range.first, it.groupValues[1].toInt(), word, picture) }
                .toList()
        }.sortedBy { it.start }
    }
