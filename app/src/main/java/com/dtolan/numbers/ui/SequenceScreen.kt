package com.dtolan.numbers.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.logic.SequenceLevel
import com.dtolan.numbers.viewmodel.SequenceViewModel

@Composable
fun SequenceScreen(language: LanguagePack, speechRate: Float, viewModel: SequenceViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val rangeStages = remember(language.code) { language.learnStages.filter { it.values == null } }
    var stageIndex by remember(language.code) { mutableIntStateOf(0.coerceIn(0, (rangeStages.size - 1).coerceAtLeast(0))) }

    LaunchedEffect(language.code, stageIndex, state.level) {
        viewModel.configure(language, stageIndex)
        viewModel.nextRound(rangeStages, speechRate)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(language.ui.sequenceModeLabel, style = MaterialTheme.typography.titleMedium)
                Text(language.ui.sequenceHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(rangeStages) { index, stage ->
                        val selected = index == stageIndex
                        Surface(
                            onClick = { stageIndex = index },
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.width(96.dp),
                        ) {
                            Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stage.title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.level == SequenceLevel.BASIC,
                        onClick = { viewModel.setLevel(SequenceLevel.BASIC) },
                        label = { Text(language.ui.sequenceLevelBasicLabel) },
                    )
                    FilterChip(
                        selected = state.level == SequenceLevel.ADVANCED,
                        onClick = { viewModel.setLevel(SequenceLevel.ADVANCED) },
                        label = { Text(language.ui.sequenceLevelAdvancedLabel) },
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    String.format(language.ui.sequenceProgressLabel, state.streak, state.bestStreak ?: 0),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { viewModel.nextRound(rangeStages, speechRate) }, enabled = !state.isPlaying) {
                        Text(language.ui.listenButton)
                    }
                    OutlinedButton(onClick = { viewModel.replay(speechRate) }, enabled = !state.isPlaying) {
                        Text(language.ui.replayButton)
                    }
                }

                if (state.choices.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.choices.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { choice ->
                                    // Must set disabledContainerColor too — these buttons become
                                    // enabled=false once answered, and a color override that only
                                    // covers the enabled state is silently ignored by Compose.
                                    val colors = when {
                                        state.answered && choice == state.correctNext.toString() -> ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1E8E3E), disabledContainerColor = Color(0xFF1E8E3E),
                                            contentColor = Color.White, disabledContentColor = Color.White,
                                        )
                                        state.answered && choice == state.selected -> ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEF4135), disabledContainerColor = Color(0xFFEF4135),
                                            contentColor = Color.White, disabledContentColor = Color.White,
                                        )
                                        else -> ButtonDefaults.buttonColors()
                                    }
                                    Button(
                                        onClick = { viewModel.chooseAnswer(choice, speechRate) },
                                        enabled = !state.answered && !state.isPlaying,
                                        colors = colors,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(choice, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                if (state.answered) {
                    NumberWordsLine(language, state.correctNext?.toString())
                    Button(onClick = { viewModel.nextRound(rangeStages, speechRate) }, modifier = Modifier.fillMaxWidth()) {
                        Text(language.ui.nextButton)
                    }
                }
            }
        }
    }
}
