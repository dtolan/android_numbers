package com.dtolan.numbers.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import com.dtolan.numbers.viewmodel.BingoViewModel

@Composable
fun BingoScreen(language: LanguagePack, speechRate: Float, viewModel: BingoViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var stageIndex by remember(language.code) {
        mutableIntStateOf(0.coerceIn(0, (language.learnStages.size - 1).coerceAtLeast(0)))
    }

    LaunchedEffect(language.code, stageIndex) {
        viewModel.configure(language, stageIndex)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(language.ui.bingoModeLabel, style = MaterialTheme.typography.titleMedium)
                Text(language.ui.bingoHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(language.learnStages) { index, stage ->
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

                Button(onClick = { viewModel.newCard(speechRate) }, modifier = Modifier.fillMaxWidth()) {
                    Text(language.ui.bingoNewCardButton)
                }
            }
        }

        if (state.board.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(String.format(language.ui.bingoCallsLabel, state.callCount), style = MaterialTheme.typography.labelMedium)
                        state.bestScore?.let {
                            Text(String.format(language.ui.bestScoreLabel, it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    state.currentCalled?.let { value ->
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(999.dp)) {
                            Text(
                                "${language.flag} $value",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                        NumberWordsLine(language, value.toString())
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        state.board.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                row.forEach { value ->
                                    val marked = value in state.marked
                                    val called = value in state.calledValues
                                    // Stay enabled=true even once marked (tapping an already-marked
                                    // cell is a harmless no-op in the ViewModel) — a Button that's
                                    // enabled=false silently ignores a containerColor override and
                                    // falls back to the default disabled grey, which would hide the
                                    // green "marked" feedback entirely.
                                    val colors = if (marked) {
                                        ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF1E8E3E), disabledContainerColor = Color(0xFF1E8E3E),
                                            contentColor = Color.White, disabledContentColor = Color.White,
                                        )
                                    } else {
                                        ButtonDefaults.buttonColors()
                                    }
                                    Button(
                                        onClick = { viewModel.markCell(value) },
                                        enabled = called && !state.won,
                                        colors = colors,
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f),
                                    ) {
                                        Text(value.toString(), style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }

                    if (state.won) {
                        Surface(color = Color(0xFF1E8E3E).copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                String.format(language.ui.bingoWinMessage, state.callCount),
                                color = Color(0xFF1E8E3E),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
