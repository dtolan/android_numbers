package com.dtolan.numbers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dtolan.numbers.data.Category
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.viewmodel.DictationViewModel

@Composable
fun DictationScreen(language: LanguagePack, speechRate: Float, viewModel: DictationViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(language.code) {
        viewModel.configure(language)
        viewModel.nextQuestion(speechRate)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(language.ui.dictationModeLabel, style = MaterialTheme.typography.titleMedium)
                Text(language.ui.dictationHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.category == Category.PHONE,
                        onClick = { viewModel.setCategory(Category.PHONE, speechRate) },
                        label = { Text(language.categoryLabels.getValue(Category.PHONE)) },
                    )
                    FilterChip(
                        selected = state.category == Category.DATE,
                        onClick = { viewModel.setCategory(Category.DATE, speechRate) },
                        label = { Text(language.categoryLabels.getValue(Category.DATE)) },
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
                    String.format(language.ui.dictationProgressLabel, state.streak, state.bestStreak ?: 0),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { viewModel.nextQuestion(speechRate) }) { Text(language.ui.listenButton) }
                    OutlinedButton(onClick = { viewModel.replay(speechRate) }, enabled = state.replaysLeft > 0) {
                        Text(language.ui.replayButton)
                    }
                }
                Text(
                    String.format(language.ui.dictationReplaysLabel, state.replaysLeft),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!state.answered) {
                    OutlinedTextField(
                        value = state.answerText,
                        onValueChange = viewModel::setAnswerText,
                        placeholder = { Text(state.question?.placeholder ?: "") },
                        singleLine = true,
                        // See PracticeCard's answer field for why autocorrect is off here.
                        keyboardOptions = KeyboardOptions(autoCorrect = false),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = viewModel::submit, modifier = Modifier.fillMaxWidth()) {
                        Text(language.ui.validateButton)
                    }
                } else {
                    val color = if (state.isCorrect) Color(0xFF1E8E3E) else Color(0xFFEF4135)
                    val text = if (state.isCorrect) {
                        String.format(language.ui.feedbackCorrect, state.question?.displayAnswer)
                    } else {
                        String.format(language.ui.feedbackIncorrect, state.question?.displayAnswer)
                    }
                    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text,
                            color = color,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                    Button(onClick = { viewModel.nextQuestion(speechRate) }, modifier = Modifier.fillMaxWidth()) {
                        Text(language.ui.nextButton)
                    }
                }
            }
        }
    }
}
