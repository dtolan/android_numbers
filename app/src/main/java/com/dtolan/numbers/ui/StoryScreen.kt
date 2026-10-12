package com.dtolan.numbers.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.StoryTheme
import com.dtolan.numbers.data.annotateNumberWords
import com.dtolan.numbers.logic.PictureCue
import com.dtolan.numbers.logic.Speaker
import com.dtolan.numbers.logic.pictureCues
import com.dtolan.numbers.viewmodel.StoryUiState
import com.dtolan.numbers.viewmodel.StoryViewModel

@Composable
fun StoryScreen(language: LanguagePack, speechRate: Float, viewModel: StoryViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val titles = language.stories.levelTitles.getValue(state.theme)
    val question = state.story?.question

    LaunchedEffect(language.code) {
        viewModel.start(language, speechRate)
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(language.ui.storyModeLabel, style = MaterialTheme.typography.titleMedium)
                Text(language.ui.storyHint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    StoryTheme.entries.forEach { theme ->
                        FilterChip(
                            selected = state.theme == theme,
                            onClick = { viewModel.setTheme(theme, speechRate) },
                            label = { Text(language.stories.themeTitles.getValue(theme)) },
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    (0 until state.levelCount).forEach { level ->
                        val locked = level > state.unlockedLevel
                        FilterChip(
                            selected = state.level == level,
                            onClick = { viewModel.setLevel(level, speechRate) },
                            enabled = !locked,
                            label = { Text("${if (locked) "🔒 " else ""}${level + 1}. ${titles[level]}") },
                        )
                    }
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
                    String.format(language.ui.storyLevelLabel, state.level + 1, titles[state.level]),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (state.isGateLevel) {
                    Text(
                        String.format(language.ui.storyProgressLabel, state.levelStreak, StoryUiState.STREAK_TO_UNLOCK),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Item stories get a picture slot: "tre mele" shows 🍎🍎🍎 as the voice says it.
                val hasPictures = remember(state.story, language.code) {
                    state.story?.let { pictureCues(it, language.stories).isNotEmpty() } ?: false
                }
                if (hasPictures) PictureFlash(state.cue)

                OutlinedButton(onClick = { viewModel.replay(speechRate) }) { Text(language.ui.replayButton) }

                if (!state.answered) {
                    OutlinedTextField(
                        value = state.answerText,
                        onValueChange = viewModel::setAnswerText,
                        placeholder = { Text(question?.placeholder ?: "") },
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
                    val format = if (state.isCorrect) language.ui.feedbackCorrect else language.ui.feedbackIncorrect
                    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            String.format(format, question?.displayAnswer),
                            color = color,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                    NumberWordsLine(language, question?.displayAnswer)
                    // After answering, show the text so the learner can check what they missed.
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(language.ui.storyTranscriptLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        state.story?.lines?.forEach { line ->
                            // Dialogue lines get a leading dash, like a script.
                            // Standalone counts get their spelling alongside: "10 (dix) pommes".
                            val spelled = annotateNumberWords(line.text, language.spellNumber, language.spellYear)
                            val text = if (line.speaker == Speaker.NARRATOR) spelled else "— $spelled"
                            Text(text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (state.justUnlocked) {
                        Text(
                            String.format(language.ui.storyUnlockedMessage, titles[state.level + 1]),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Button(onClick = { viewModel.setLevel(state.level + 1, speechRate) }, modifier = Modifier.fillMaxWidth()) {
                            Text(language.ui.storyNextLevelButton)
                        }
                        OutlinedButton(onClick = { viewModel.nextQuestion(speechRate) }, modifier = Modifier.fillMaxWidth()) {
                            Text(language.ui.nextButton)
                        }
                    } else {
                        Button(onClick = { viewModel.nextQuestion(speechRate) }, modifier = Modifier.fillMaxWidth()) {
                            Text(language.ui.nextButton)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The quantity as pictures — 🍎🍎🍎 while the voice says "tre mele" — popping in as the number is
 * spoken. Large counts shrink the pictures to fit; a fixed-height slot keeps the layout still.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PictureFlash(cue: PictureCue?) {
    AnimatedContent(
        targetState = cue,
        transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith fadeOut() },
        contentAlignment = Alignment.Center,
        label = "pictureFlash",
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
    ) { current ->
        if (current != null) {
            val style = when {
                current.count <= 6 -> MaterialTheme.typography.displaySmall
                current.count <= 15 -> MaterialTheme.typography.headlineMedium
                else -> MaterialTheme.typography.titleMedium
            }
            FlowRow(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                repeat(current.count.coerceAtMost(MAX_PICTURES)) { Text(current.picture, style = style) }
            }
        }
    }
}

/** Past this a row of pictures stops being countable at a glance; story counts stay well under it. */
private const val MAX_PICTURES = 40
