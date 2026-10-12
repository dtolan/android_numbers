package com.dtolan.numbers.listen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.dtolan.numbers.MainActivity
import com.dtolan.numbers.data.LanguagePack
import com.dtolan.numbers.data.Languages
import com.dtolan.numbers.data.LearnStage
import com.dtolan.numbers.logic.NumberSource
import com.dtolan.numbers.logic.RetentionQueue
import com.dtolan.numbers.logic.SequentialSource
import com.dtolan.numbers.logic.ShuffleSource
import com.dtolan.numbers.logic.CountDirection
import com.dtolan.numbers.logic.countValues
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * How the passive Listen drill picks its next value.
 * - [SPACED_REPETITION]: Pimsleur-style graduated-interval recall (the original/default) — new
 *   values repeat on a growing schedule while newer ones keep entering, for active memorization.
 * - [SEQUENTIAL]: plays straight through the range in order, looping — for systematically hearing
 *   every number, e.g. walking the irregular 70-99 zone in order.
 * - [RANDOM_SHUFFLE]: plain shuffled passes with no spacing logic — simple passive exposure.
 * - [ALWAYS_CONFIRM]: spaced repetition, but the native-language echo plays on every repeat
 *   (not just the first couple) — for a beginner who still needs the translation as a crutch.
 */
enum class ListeningMode { SPACED_REPETITION, SEQUENTIAL, RANDOM_SHUFFLE, ALWAYS_CONFIRM }

/** Current state of the passive listening drill, shared in-process with the UI. */
data class ListeningState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val languageCode: String = "",
    val stageTitle: String = "",
    val mode: ListeningMode = ListeningMode.SPACED_REPETITION,
    val confirmLanguageCode: String? = null,
    val currentValue: Int? = null,
    /** True for a plain Count session (range in order, no echo) rather than a Listen drill. */
    val isCount: Boolean = false,
    /** Count over the years scale: numbers are read as years ("nineteen fifty-four" in English). */
    val asYears: Boolean = false,
) {
    /** [value] written out the way this session says it. */
    fun spell(lang: LanguagePack, value: Int): String = if (asYears) lang.spellYear(value) else lang.spellNumber(value)
}

/**
 * Runs the passive "listen while doing something else" drill as a foreground service, so it
 * keeps speaking with the screen off or the app backgrounded (e.g. mid-run) — a plain
 * Activity-scoped TTS loop would get killed as soon as the app leaves the foreground.
 *
 * Owns its own [TextToSpeech] instance (independent of the one the UI uses) and drives a
 * [RetentionQueue] over the chosen [com.dtolan.numbers.data.LearnStage]'s range, speaking one
 * value at a time with a pause in between, optionally followed by the same value spoken in a
 * second ("confirm") language. Controlled via the notification's Pause/Resume/Stop actions, which
 * re-enter this service through [onStartCommand] rather than needing a bound-service connection.
 */
class ListeningService : Service() {

    private lateinit var tts: TextToSpeech
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val ttsReady = CompletableDeferred<Boolean>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var paused = false

    /** Keeps the CPU awake through Doze while actively drilling — screen-off during a run is the primary use case. */
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this) { status -> ttsReady.complete(status == TextToSpeech.SUCCESS) }
        val channel = NotificationChannel(CHANNEL_ID, "Passive listening drill", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "numbers:listening-drill")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_START_COUNT -> handleStartCount(intent)
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_STOP -> handleStop()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        val languageCode = intent.getStringExtra(EXTRA_LANGUAGE) ?: return
        val stageIndex = intent.getIntExtra(EXTRA_STAGE_INDEX, 0)
        val confirmLanguageCode = intent.getStringExtra(EXTRA_CONFIRM_LANGUAGE)
        val speechRate = intent.getFloatExtra(EXTRA_SPEECH_RATE, 1.0f)
        val mode = intent.getStringExtra(EXTRA_MODE)?.let { runCatching { ListeningMode.valueOf(it) }.getOrNull() }
            ?: ListeningMode.SPACED_REPETITION
        val lang = Languages.byCode(languageCode)
        val stage = lang.learnStages.getOrNull(stageIndex) ?: lang.learnStages.firstOrNull() ?: return

        job?.cancel()
        paused = false
        val source = buildSource(stage, mode)

        _state.value = ListeningState(
            isRunning = true,
            isPaused = false,
            languageCode = languageCode,
            stageTitle = stage.title,
            mode = mode,
            confirmLanguageCode = confirmLanguageCode,
            currentValue = null,
        )
        startForegroundCompat(buildNotification(lang, stage.title, paused = false))
        wakeLock?.let { if (!it.isHeld) it.acquire(MAX_WAKE_LOCK_MS) }

        job = serviceScope.launch {
            ttsReady.await()
            while (isActive) {
                if (paused) {
                    delay(300)
                    continue
                }
                val pick = source.next()
                val value = pick.value
                _state.value = _state.value.copy(currentValue = value)
                updateNotification(lang, stage.title, paused = false)

                speakAndAwait(value.toString(), lang.locale, speechRate)
                if (paused) continue
                delay(PAUSE_MS)

                // Translate for the first couple of exposures (bootstrapping a brand-new number) —
                // every later spaced repeat stays target-language-only, so it tests recall instead
                // of just retraining "wait for the translation". ALWAYS_CONFIRM opts out of that
                // fade-out entirely, for a beginner who still needs the translation every time.
                val shouldConfirm = mode == ListeningMode.ALWAYS_CONFIRM || pick.exposureNumber <= CONFIRM_EXPOSURE_LIMIT
                if (confirmLanguageCode != null && shouldConfirm && !paused) {
                    val confirmLang = Languages.byCode(confirmLanguageCode)
                    speakAndAwait(value.toString(), confirmLang.locale, speechRate)
                    delay(CONFIRM_PAUSE_MS)
                }
            }
        }
    }

    /**
     * Count mode: speaks every number from one end of a range to the other with only a short gap,
     * no translation echo — pure passive exposure — up, down, or oscillating up and down. Loops back to the start when [EXTRA_LOOP] is set,
     * otherwise stops the service after the last number.
     */
    private fun handleStartCount(intent: Intent) {
        val languageCode = intent.getStringExtra(EXTRA_LANGUAGE) ?: return
        val from = intent.getIntExtra(EXTRA_FROM, 1)
        val to = intent.getIntExtra(EXTRA_TO, 100)
        val direction = intent.getStringExtra(EXTRA_DIRECTION)?.let { CountDirection.valueOf(it) } ?: CountDirection.UP
        val loop = intent.getBooleanExtra(EXTRA_LOOP, true)
        val asYears = intent.getBooleanExtra(EXTRA_AS_YEARS, false)
        val speechRate = intent.getFloatExtra(EXTRA_SPEECH_RATE, 1.0f)
        val lang = Languages.byCode(languageCode)
        val values = countValues(from, to, direction, loop)
        val title = if (direction == CountDirection.UP_DOWN) {
            "${values.first()} ⇄ ${values.maxOrNull()}"
        } else {
            "${values.first()} → ${values.last()}"
        }

        job?.cancel()
        paused = false
        _state.value = ListeningState(isRunning = true, languageCode = languageCode, stageTitle = title, isCount = true, asYears = asYears)
        startForegroundCompat(buildNotification(lang, title, paused = false))
        wakeLock?.let { if (!it.isHeld) it.acquire(MAX_WAKE_LOCK_MS) }

        job = serviceScope.launch {
            ttsReady.await()
            var index = 0
            while (isActive) {
                if (paused) {
                    delay(300)
                    continue
                }
                if (index >= values.size) {
                    if (!loop) {
                        // Stop from the main thread: handleStop cancels this very job.
                        mainHandler.post { handleStop() }
                        break
                    }
                    index = 0
                }
                val value = values[index]
                _state.value = _state.value.copy(currentValue = value)
                updateNotification(lang, title, paused = false)
                // A bare "1954" is read "one thousand nine hundred fifty-four" by the English voice,
                // so where a language says years differently, the voice gets the year in words.
                val yearWords = lang.spellYear(value).takeIf { asYears && it != lang.spellNumber(value) }
                speakAndAwait(yearWords ?: value.toString(), lang.locale, speechRate)
                if (paused) continue // re-speak the interrupted number on resume
                index++
                delay(COUNT_PAUSE_MS)
            }
        }
    }

    private fun buildSource(stage: LearnStage, mode: ListeningMode): NumberSource {
        val values = stage.values ?: (stage.min..stage.max).toList()
        return when (mode) {
            ListeningMode.SEQUENTIAL -> SequentialSource(values)
            ListeningMode.RANDOM_SHUFFLE -> ShuffleSource(values)
            ListeningMode.SPACED_REPETITION, ListeningMode.ALWAYS_CONFIRM ->
                RetentionQueue(source = { values[Random.nextInt(values.size)] })
        }
    }

    private suspend fun speakAndAwait(text: String, locale: java.util.Locale, rate: Float) {
        val expectedId = UUID.randomUUID().toString()
        suspendCancellableCoroutine<Unit> { continuation ->
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == expectedId && continuation.isActive) continuation.resume(Unit)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == expectedId && continuation.isActive) continuation.resume(Unit)
                }
                // tts.stop() (Pause) reports onStop, not onDone — without this the loop would wait forever.
                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    if (utteranceId == expectedId && continuation.isActive) continuation.resume(Unit)
                }
            })
            tts.language = locale
            tts.setSpeechRate(rate)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, expectedId)
        }
    }

    private fun handlePause() {
        paused = true
        tts.stop()
        _state.value = _state.value.copy(isPaused = true)
        if (_state.value.isRunning) {
            val lang = Languages.byCode(_state.value.languageCode)
            updateNotification(lang, _state.value.stageTitle, paused = true)
        }
    }

    private fun handleResume() {
        paused = false
        _state.value = _state.value.copy(isPaused = false)
        val lang = Languages.byCode(_state.value.languageCode)
        updateNotification(lang, _state.value.stageTitle, paused = false)
    }

    private fun handleStop() {
        job?.cancel()
        job = null
        paused = false
        tts.stop()
        releaseWakeLock()
        _state.value = ListeningState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        job?.cancel()
        tts.stop()
        tts.shutdown()
        releaseWakeLock()
        _state.value = ListeningState()
        super.onDestroy()
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(lang: LanguagePack, stageTitle: String, paused: Boolean) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(lang, stageTitle, paused))
    }

    private fun buildNotification(lang: LanguagePack, stageTitle: String, paused: Boolean): android.app.Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val pauseResumeAction = if (paused) {
            NotificationCompat.Action(android.R.drawable.ic_media_play, "Resume", servicePendingIntent(ACTION_RESUME))
        } else {
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", servicePendingIntent(ACTION_PAUSE))
        }
        val stopAction = NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "Stop", servicePendingIntent(ACTION_STOP))

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("${if (_state.value.isCount) "🔁" else "🎧"} ${lang.flag} ${lang.displayName} — $stageTitle")
            // The current number, written out, so it can be read at a glance from the lock screen.
            .setContentText(
                when {
                    paused -> "Paused"
                    else -> _state.value.currentValue?.let { "$it · ${_state.value.spell(lang, it)}" } ?: "Listening…"
                },
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun servicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, ListeningService::class.java).setAction(action)
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        private const val CHANNEL_ID = "listening_drill"
        private const val NOTIFICATION_ID = 4201
        private const val PAUSE_MS = 2200L
        private const val CONFIRM_PAUSE_MS = 1600L
        private const val COUNT_PAUSE_MS = 350L
        private const val CONFIRM_EXPOSURE_LIMIT = 2
        private const val MAX_WAKE_LOCK_MS = 3 * 60 * 60 * 1000L // 3h safety cap, in case stop is never called

        private const val ACTION_START = "com.dtolan.numbers.listen.START"
        private const val ACTION_PAUSE = "com.dtolan.numbers.listen.PAUSE"
        private const val ACTION_RESUME = "com.dtolan.numbers.listen.RESUME"
        private const val ACTION_STOP = "com.dtolan.numbers.listen.STOP"
        private const val ACTION_START_COUNT = "com.dtolan.numbers.listen.START_COUNT"
        private const val EXTRA_FROM = "from"
        private const val EXTRA_TO = "to"
        private const val EXTRA_DIRECTION = "direction"
        private const val EXTRA_LOOP = "loop"
        private const val EXTRA_AS_YEARS = "asYears"
        private const val EXTRA_LANGUAGE = "language"
        private const val EXTRA_STAGE_INDEX = "stageIndex"
        private const val EXTRA_CONFIRM_LANGUAGE = "confirmLanguage"
        private const val EXTRA_SPEECH_RATE = "speechRate"
        private const val EXTRA_MODE = "mode"

        private val _state = MutableStateFlow(ListeningState())
        val state: StateFlow<ListeningState> = _state.asStateFlow()

        fun startIntent(
            context: Context,
            languageCode: String,
            stageIndex: Int,
            confirmLanguageCode: String?,
            speechRate: Float,
            mode: ListeningMode = ListeningMode.SPACED_REPETITION,
        ): Intent =
            Intent(context, ListeningService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_LANGUAGE, languageCode)
                putExtra(EXTRA_STAGE_INDEX, stageIndex)
                putExtra(EXTRA_SPEECH_RATE, speechRate)
                putExtra(EXTRA_MODE, mode.name)
                confirmLanguageCode?.let { putExtra(EXTRA_CONFIRM_LANGUAGE, it) }
            }

        fun countIntent(
            context: Context,
            languageCode: String,
            from: Int,
            to: Int,
            direction: CountDirection,
            loop: Boolean,
            asYears: Boolean,
            speechRate: Float,
        ): Intent =
            Intent(context, ListeningService::class.java).apply {
                action = ACTION_START_COUNT
                putExtra(EXTRA_LANGUAGE, languageCode)
                putExtra(EXTRA_FROM, from)
                putExtra(EXTRA_TO, to)
                putExtra(EXTRA_DIRECTION, direction.name)
                putExtra(EXTRA_LOOP, loop)
                putExtra(EXTRA_AS_YEARS, asYears)
                putExtra(EXTRA_SPEECH_RATE, speechRate)
            }

        fun pauseIntent(context: Context): Intent = Intent(context, ListeningService::class.java).setAction(ACTION_PAUSE)
        fun resumeIntent(context: Context): Intent = Intent(context, ListeningService::class.java).setAction(ACTION_RESUME)
        fun stopIntent(context: Context): Intent = Intent(context, ListeningService::class.java).setAction(ACTION_STOP)
    }
}
