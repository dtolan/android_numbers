package com.dtolan.numbers.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dtolan.numbers.listen.ListeningMode
import com.dtolan.numbers.listen.ListeningService
import com.dtolan.numbers.logic.CountDirection
import com.dtolan.numbers.tts.TtsController
import com.dtolan.numbers.viewmodel.AppMode
import com.dtolan.numbers.viewmodel.PracticeViewModel

@Composable
fun NumbersApp(viewModel: PracticeViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val listeningState by ListeningService.state.collectAsState()
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    fun startCounting(from: Int, to: Int, direction: CountDirection, loop: Boolean, asYears: Boolean) {
        requestNotificationPermission()
        ContextCompat.startForegroundService(
            context,
            ListeningService.countIntent(context, state.language.code, from, to, direction, loop, asYears, state.settings.speechRate),
        )
    }
    fun startListening(stageIndex: Int, mode: ListeningMode) {
        requestNotificationPermission()
        val confirmCode = state.confirmLanguageCode?.takeIf { it != state.language.code }
        ContextCompat.startForegroundService(
            context,
            ListeningService.startIntent(context, state.language.code, stageIndex, confirmCode, state.settings.speechRate, mode),
        )
    }

    if (showSettings) {
        AppSettingsSheet(
            language = state.language,
            speechRate = state.settings.speechRate,
            confirmLanguageCode = state.confirmLanguageCode,
            onRateChange = viewModel::setSpeechRate,
            onConfirmLanguageChange = viewModel::setConfirmLanguage,
            onDismiss = { showSettings = false },
        )
    }

    // System back returns to the hub instead of closing the app from inside a mode.
    BackHandler(enabled = state.mode != AppMode.HOME) { viewModel.setMode(AppMode.HOME) }

    // While the background player runs, keep its controls reachable from every other screen.
    val showNowPlaying = listeningState.isRunning &&
        !(state.mode == AppMode.LISTEN && !listeningState.isCount) &&
        !(state.mode == AppMode.COUNT && listeningState.isCount)

    Scaffold(
        bottomBar = {
            if (showNowPlaying) {
                NowPlayingBar(
                    state = listeningState,
                    onOpen = { viewModel.setMode(if (listeningState.isCount) AppMode.COUNT else AppMode.LISTEN) },
                    onPause = { context.startService(ListeningService.pauseIntent(context)) },
                    onResume = { context.startService(ListeningService.resumeIntent(context)) },
                    onStop = { context.startService(ListeningService.stopIntent(context)) },
                )
            }
        },
    ) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.mode == AppMode.HOME) {
                    // Home: the full language header and picker.
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "${state.language.flag} ${state.language.displayName}",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                            )
                            Text(state.language.ui.subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        }
                        IconButton(onClick = { showSettings = true }, modifier = Modifier.align(Alignment.TopEnd)) {
                            Icon(Icons.Filled.Settings, contentDescription = state.language.ui.settingsTitle)
                        }
                    }
                    LanguagePicker(language = state.language, onLanguageChange = viewModel::setLanguage)
                } else {
                    // Inside a mode: a compact bar, so the game itself starts near the top.
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { viewModel.setMode(AppMode.HOME) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = state.language.ui.homeTitle)
                        }
                        Text(
                            modeTitle(state.language, state.mode),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        LanguagePicker(language = state.language, onLanguageChange = viewModel::setLanguage, compact = true)
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = state.language.ui.settingsTitle)
                        }
                    }
                }

                val learnStage = state.language.learnStages.getOrNull(state.learnStageIndex)
                val progressText = if (state.mode == AppMode.LEARN && learnStage != null) {
                    String.format(
                        state.language.ui.learnStageProgress,
                        state.learnStreak.coerceAtMost(learnStage.requiredStreak),
                        learnStage.requiredStreak,
                        state.learnTimeLimitMs / 1000.0,
                    )
                } else null

                val practiceCard = @Composable {
                    PracticeCard(
                        language = state.language,
                        question = state.currentQuestion,
                        answerText = state.answerText,
                        feedback = state.feedback,
                        showAnswerForm = state.showAnswerForm,
                        voiceAvailable = state.voiceAvailable,
                        onAnswerChange = viewModel::setAnswerText,
                        onListen = { if (state.currentQuestion == null) viewModel.nextQuestion() else viewModel.replay() },
                        onReplay = viewModel::replay,
                        onSubmit = viewModel::submitAnswer,
                        onNext = viewModel::nextQuestion,
                        onInstallVoice = { context.startActivity(TtsController.installVoiceDataIntent()) },
                        progressText = progressText,
                        nextButtonLabel = if (state.learnJustCompletedStage) state.language.ui.learnNextStageButton else null,
                        onChooseAnswer = if (state.mode == AppMode.LEARN) viewModel::chooseAnswer else null,
                        timeLimitMs = state.learnTimeLimitMs,
                        previewActive = state.mode == AppMode.LEARN && state.learnPreviewActive,
                    )
                }

                when (state.mode) {
                    AppMode.HOME -> {
                        HomeScreen(language = state.language, onSelect = viewModel::setMode)
                    }
                    AppMode.LEARN -> {
                        // The game itself comes first, fully visible with no scrolling — the
                        // stage picker below is a compact horizontal strip, never a tall list
                        // that could push the game off-screen.
                        practiceCard()
                        LearnPanel(
                            language = state.language,
                            stageIndex = state.learnStageIndex,
                            unlockedIndex = state.learnUnlockedIndex,
                            onSelectStage = viewModel::selectLearnStage,
                        )
                    }
                    AppMode.LISTEN -> {
                        ListenPanel(
                            language = state.language,
                            initialStageIndex = state.learnStageIndex,
                            listeningState = listeningState,
                            confirmLanguageCode = state.confirmLanguageCode,
                            onStart = ::startListening,
                            onPause = { context.startService(ListeningService.pauseIntent(context)) },
                            onResume = { context.startService(ListeningService.resumeIntent(context)) },
                            onStop = { context.startService(ListeningService.stopIntent(context)) },
                        )
                    }
                    AppMode.PRACTICE -> {
                        practiceCard()
                        ScoreBar(
                            language = state.language,
                            correct = state.correct,
                            total = state.total,
                            streak = state.streak,
                            onReset = viewModel::resetScore,
                        )
                        SettingsPanel(
                            language = state.language,
                            settings = state.settings,
                            onCategoryToggle = viewModel::toggleCategory,
                            onRangesChange = viewModel::updateRanges,
                        )
                    }
                    AppMode.BINGO -> BingoScreen(language = state.language, speechRate = state.settings.speechRate)
                    AppMode.SEQUENCE -> SequenceScreen(language = state.language, speechRate = state.settings.speechRate)
                    AppMode.DICTATION -> DictationScreen(language = state.language, speechRate = state.settings.speechRate)
                    AppMode.SIMON -> SimonScreen(language = state.language, speechRate = state.settings.speechRate)
                    AppMode.STORIES -> StoryScreen(language = state.language, speechRate = state.settings.speechRate)
                    AppMode.SPEAK -> SpeakScreen(language = state.language, speechRate = state.settings.speechRate)
                    AppMode.COUNT -> CountPanel(
                        language = state.language,
                        listeningState = listeningState,
                        onStart = ::startCounting,
                        onPause = { context.startService(ListeningService.pauseIntent(context)) },
                        onResume = { context.startService(ListeningService.resumeIntent(context)) },
                        onStop = { context.startService(ListeningService.stopIntent(context)) },
                    )
                }
            }
        }
    }
}

private fun modeTitle(language: com.dtolan.numbers.data.LanguagePack, mode: AppMode): String = with(language.ui) {
    when (mode) {
        AppMode.HOME -> homeTitle
        AppMode.PRACTICE -> practiceModeLabel
        AppMode.LEARN -> learnModeLabel
        AppMode.LISTEN -> listenModeLabel
        AppMode.COUNT -> countModeLabel
        AppMode.STORIES -> storyModeLabel
        AppMode.SPEAK -> language.speak.modeLabel
        AppMode.BINGO -> bingoModeLabel
        AppMode.SEQUENCE -> sequenceModeLabel
        AppMode.DICTATION -> dictationModeLabel
        AppMode.SIMON -> simonModeLabel
    }
}
