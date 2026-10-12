package com.dtolan.numbers.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dtolan.numbers.data.LanguagePack

/**
 * Compact stage picker: a fixed-height horizontally-scrolling strip (a "scrolling window") of
 * stage pills, not a tall vertical list — so it never pushes the practice card itself off-screen,
 * no matter how many stages a language's curriculum has. The full description of only the
 * *currently selected* stage is shown below the strip.
 */
@Composable
fun LearnPanel(
    language: LanguagePack,
    stageIndex: Int,
    unlockedIndex: Int,
    onSelectStage: (Int) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(language.ui.learnModeLabel, style = MaterialTheme.typography.titleMedium)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(language.learnStages) { index, stage ->
                    val locked = index > unlockedIndex
                    val mastered = index < unlockedIndex
                    val selected = index == stageIndex
                    StagePill(
                        title = stage.title,
                        locked = locked,
                        mastered = mastered,
                        selected = selected,
                        onClick = { onSelectStage(index) },
                    )
                }
            }

            language.learnStages.getOrNull(stageIndex)?.let { current ->
                Text(current.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StagePill(
    title: String,
    locked: Boolean,
    mastered: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = !locked,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.width(96.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(if (mastered) "✓" else if (locked) "🔒" else "▶", style = MaterialTheme.typography.titleSmall)
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (locked) 0.5f else 1f),
            )
        }
    }
}
