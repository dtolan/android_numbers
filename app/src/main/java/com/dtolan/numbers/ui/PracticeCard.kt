package com.dtolan.numbers.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.logic.Question
import com.dtolan.numbers.viewmodel.FeedbackState
import kotlinx.coroutines.delay

@Composable
fun PracticeCard(
    language: LanguagePack,
    question: Question?,
    answerText: String,
    feedback: FeedbackState,
    showAnswerForm: Boolean,
    voiceAvailable: Boolean,
    onAnswerChange: (String) -> Unit,
    onListen: () -> Unit,
    onReplay: () -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    onInstallVoice: () -> Unit,
    progressText: String? = null,
    nextButtonLabel: String? = null,
    onChooseAnswer: ((String) -> Unit)? = null,
    timeLimitMs: Long = 0,
    previewActive: Boolean = false,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!voiceAvailable) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(language.ui.voiceMissingTitle, style = MaterialTheme.typography.titleSmall)
                        Text(String.format(language.ui.voiceMissingBody, language.displayName), style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = onInstallVoice) { Text(language.ui.installVoiceButton) }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = question?.label ?: language.ui.chooseCategoryWarning,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            if (previewActive) {
                Text(language.ui.learnGetReadyLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            } else if (progressText != null) {
                Text(progressText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onListen, enabled = question != null) { Text(language.ui.listenButton) }
                OutlinedButton(onClick = onReplay, enabled = question != null) { Text(language.ui.replayButton) }
            }

            if (question != null && question.choices != null && onChooseAnswer != null) {
                if (showAnswerForm) {
                    CountdownBar(question = question, timeLimitMs = timeLimitMs, active = !previewActive)
                }
                ChoiceGrid(
                    choices = question.choices,
                    correctAnswer = question.displayAnswer,
                    selected = answerText,
                    answered = !showAnswerForm,
                    interactive = !previewActive,
                    onChoose = onChooseAnswer,
                )
            } else if (showAnswerForm && question != null) {
                OutlinedTextField(
                    value = answerText,
                    onValueChange = onAnswerChange,
                    placeholder = { Text(question.placeholder) },
                    singleLine = true,
                    // Autocorrect keeps an uncommitted "composing" span in the IME that some
                    // keyboards drop on focus loss (e.g. tapping Listen/Repeat mid-entry) — for a
                    // plain number/phone/date answer there's nothing to correct, so turn it off to
                    // stop that from silently eating typed digits.
                    keyboardOptions = KeyboardOptions(autoCorrect = false),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) {
                    Text(language.ui.validateButton)
                }
            }

            when (feedback) {
                is FeedbackState.Correct -> FeedbackText(String.format(language.ui.feedbackCorrect, feedback.displayAnswer), Color(0xFF1E8E3E))
                is FeedbackState.Incorrect -> FeedbackText(String.format(language.ui.feedbackIncorrect, feedback.displayAnswer), Color(0xFFEF4135))
                is FeedbackState.StageComplete -> FeedbackText(feedback.message, Color(0xFF1E8E3E))
                is FeedbackState.TooSlow -> FeedbackText(
                    String.format(language.ui.learnTooSlowFeedback, feedback.displayAnswer, feedback.elapsedMs / 1000.0, feedback.limitMs / 1000.0),
                    Color(0xFFB8860B),
                )
                FeedbackState.None -> {}
            }
            if (feedback != FeedbackState.None) {
                NumberWordsLine(language, question?.displayAnswer)
            }

            if (!showAnswerForm && question != null) {
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                    Text(nextButtonLabel ?: language.ui.nextButton)
                }
            }
        }
    }
}

@Composable
private fun ChoiceGrid(
    choices: List<String>,
    correctAnswer: String,
    selected: String,
    answered: Boolean,
    interactive: Boolean,
    onChoose: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        choices.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { choice ->
                    // Buttons here become enabled=false once answered, so a color override must
                    // also cover disabledContainerColor — otherwise Compose falls back to the
                    // default disabled grey and the correct/wrong feedback silently never shows.
                    val colors = when {
                        answered && choice == correctAnswer -> ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E8E3E), disabledContainerColor = Color(0xFF1E8E3E),
                            contentColor = Color.White, disabledContentColor = Color.White,
                        )
                        answered && choice == selected -> ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4135), disabledContainerColor = Color(0xFFEF4135),
                            contentColor = Color.White, disabledContentColor = Color.White,
                        )
                        else -> ButtonDefaults.buttonColors()
                    }
                    Button(
                        onClick = { onChoose(choice) },
                        enabled = !answered && interactive,
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

/**
 * Visual-only countdown of the current adaptive time limit; the actual timing is measured in the
 * ViewModel. Stays full (not ticking) while [active] is false, e.g. during the pre-speech preview
 * beat, then counts down for real once the word is spoken and the clock actually starts.
 */
@Composable
private fun CountdownBar(question: Question, timeLimitMs: Long, active: Boolean) {
    var progress by remember(question) { mutableFloatStateOf(1f) }
    LaunchedEffect(question, timeLimitMs, active) {
        if (!active || timeLimitMs <= 0) return@LaunchedEffect
        val stepMs = 50L
        var elapsed = 0L
        while (elapsed < timeLimitMs) {
            delay(stepMs)
            elapsed += stepMs
            progress = (1f - elapsed.toFloat() / timeLimitMs).coerceAtLeast(0f)
        }
    }
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun FeedbackText(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
