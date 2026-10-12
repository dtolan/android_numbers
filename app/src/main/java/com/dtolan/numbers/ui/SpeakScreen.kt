package com.dtolan.numbers.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.logic.SpeakGenerator
import com.dtolan.numbers.viewmodel.SpeakViewModel

/**
 * Speak mode: see or hear a prompt, tap the mic and answer out loud. Uses the system
 * SpeechRecognizer (on-device on recent Pixels once the language's offline speech pack is
 * installed) and asks for several guesses, any of which may match.
 */
@Composable
fun SpeakScreen(language: LanguagePack, speechRate: Float, viewModel: SpeakViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val s = language.speak
    val task = state.task

    LaunchedEffect(language.code) { viewModel.start(language, speechRate) }
    var confirmSkip by remember { mutableStateOf(false) }
    if (confirmSkip) {
        AlertDialog(
            onDismissRequest = { confirmSkip = false },
            title = { Text(s.skipConfirmTitle) },
            text = { Text(s.skipConfirmBody) },
            confirmButton = {
                TextButton(onClick = { confirmSkip = false; viewModel.next(speechRate) }) { Text(s.skipConfirmYes) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSkip = false; viewModel.retry() }) { Text(s.retryButton) }
            },
        )
    }

    val available = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    val recognizer = remember { if (available) SpeechRecognizer.createSpeechRecognizer(context) else null }
    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = viewModel.onListeningStarted()
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() = viewModel.onListeningStopped()
            override fun onError(error: Int) {
                val noSpeech = error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                viewModel.onRecognitionProblem(if (noSpeech) s.noSpeechMessage else String.format(s.errorMessage, error))
            }
            override fun onResults(results: Bundle?) {
                val guesses = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                if (guesses.isEmpty()) viewModel.onRecognitionProblem(s.noSpeechMessage) else viewModel.onHeard(guesses)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose { recognizer?.destroy() }
    }

    fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, s.recognizerTag)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        viewModel.onListeningStarted()
        recognizer?.startListening(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) listen() else viewModel.onRecognitionProblem(s.permissionMessage)
    }
    fun onMic() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            listen()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    String.format(language.ui.storyLevelLabel, state.level + 1, s.levelTitles[state.level]),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (state.streak > 0) {
                    Text(
                        "${language.ui.streakLabel} ${state.streak}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (task != null) {
                    Text(task.instruction, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Text(task.prompt, style = MaterialTheme.typography.displayMedium)
                    if (task.speakText != null) {
                        OutlinedButton(onClick = { viewModel.replay(speechRate) }) { Text(language.ui.replayButton) }
                    } else if (!state.answered) {
                        // Seen prompts can be heard first; heard ones (maths, positions) only after
                        // answering, or the voice would give the answer away.
                        TextButton(onClick = { viewModel.sayAnswer(speechRate) }) { Text(s.hearAnswerButton) }
                    }
                }

                if (!available) {
                    Text(s.unavailableMessage, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                } else if (!state.answered) {
                    Button(
                        onClick = ::onMic,
                        enabled = !state.isListening && task != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                    ) {
                        Text(if (state.isListening) s.listeningLabel else s.micButton, style = MaterialTheme.typography.titleLarge)
                    }
                }

                state.message?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    // Nothing usable was heard, so there's no answer to move on from — offer a way out.
                    if (!state.answered) {
                        TextButton(onClick = { confirmSkip = true }) { Text(s.skipConfirmYes) }
                    }
                }

                if (state.answered && task != null) {
                    val color = if (state.isCorrect) Color(0xFF1E8E3E) else Color(0xFFEF4135)
                    val format = if (state.isCorrect) language.ui.feedbackCorrect else language.ui.feedbackIncorrect
                    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text(String.format(format, task.answerLabel(language)), color = color, textAlign = TextAlign.Center)
                            state.heard?.let {
                                Text(String.format(s.heardLabel, it), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            }
                        }
                    }
                    Text(
                        task.answerWords(language),
                        style = MaterialTheme.typography.titleMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedButton(onClick = { viewModel.sayAnswer(speechRate) }) { Text(s.hearAnswerButton) }
                    // Heard tasks: show what was said, so a miss can be checked against the text.
                    task.speakText?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                    if (!state.isCorrect) {
                        Text(s.repeatToContinue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        // Offered after a right answer too: saying it again is good practice.
                        OutlinedButton(onClick = viewModel::retry, modifier = Modifier.weight(1f)) { Text(s.retryButton) }
                        // After a miss, moving on is allowed but asks first — the aim is to say it right.
                        Button(
                            onClick = { if (state.isCorrect) viewModel.next(speechRate) else confirmSkip = true },
                            modifier = Modifier.weight(1f),
                        ) { Text(language.ui.nextButton) }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    (0 until SpeakGenerator.LEVEL_COUNT).forEach { level ->
                        FilterChip(
                            selected = state.level == level,
                            onClick = { viewModel.setLevel(level, speechRate) },
                            label = { Text("${level + 1}. ${s.levelTitles[level]}") },
                        )
                    }
                }
            }
        }
    }
}
