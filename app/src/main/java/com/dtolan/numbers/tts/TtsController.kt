package com.dtolan.numbers.tts

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Thin wrapper around Android's built-in [TextToSpeech] engine. This speaks fully offline once
 * the device has the requested language's voice data installed (Settings > Accessibility >
 * Text-to-speech, or via [installVoiceDataIntent]) — no network access is used at runtime.
 */
class TtsController(context: Context) {
    private val appContext = context.applicationContext
    private var engine: TextToSpeech? = null
    private var engineReady = false
    private val pendingActions = mutableListOf<() -> Unit>()

    /** Fires once the underlying engine has finished initializing (success or failure). */
    var onEngineReady: (() -> Unit)? = null

    init {
        engine = TextToSpeech(appContext) { status ->
            engineReady = status == TextToSpeech.SUCCESS
            if (engineReady) {
                pendingActions.forEach { it() }
                pendingActions.clear()
            }
            onEngineReady?.invoke()
        }
    }

    private fun whenReady(action: () -> Unit) {
        if (engineReady) action() else pendingActions.add(action)
    }

    /** [TextToSpeech.LANG_AVAILABLE] and friends; negative values mean unsupported/missing data. */
    fun languageStatus(locale: Locale): Int {
        val e = engine ?: return TextToSpeech.LANG_NOT_SUPPORTED
        return if (engineReady) e.isLanguageAvailable(locale) else TextToSpeech.LANG_NOT_SUPPORTED
    }

    fun hasUsableVoice(locale: Locale): Boolean = languageStatus(locale) >= TextToSpeech.LANG_AVAILABLE

    fun speak(text: String, locale: Locale, rate: Float, onDone: (() -> Unit)? = null) {
        whenReady {
            val e = engine ?: return@whenReady
            e.language = locale
            e.setSpeechRate(rate)
            if (onDone != null) {
                e.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        onDone()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {}
                })
            }
            e.stop()
            e.setPitch(1f)
            e.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        }
    }

    /**
     * Speaks [lines] back to back, each at its own pitch — lets a single voice play several
     * characters in a dialogue. Pitch is captured per utterance when it is queued, so it is reset
     * to normal afterwards for everything else that speaks.
     *
     * [onProgress] reports where the voice is: (line, character offset) as each line starts and,
     * on engines that support it, as each word starts; (lines.size, 0) once the last line ends.
     * It's called on the TTS engine's thread.
     */
    fun speakLines(
        lines: List<Pair<String, Float>>,
        locale: Locale,
        rate: Float,
        onProgress: ((line: Int, offset: Int) -> Unit)? = null,
    ) {
        whenReady {
            val e = engine ?: return@whenReady
            e.language = locale
            e.setSpeechRate(rate)
            e.stop()
            val batch = UUID.randomUUID().toString()
            fun lineOf(utteranceId: String?): Int? =
                utteranceId?.takeIf { it.startsWith("$batch:") }?.substringAfter(':')?.toIntOrNull()
            if (onProgress != null) {
                e.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        lineOf(utteranceId)?.let { onProgress(it, 0) }
                    }
                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                        lineOf(utteranceId)?.let { onProgress(it, start) }
                    }
                    override fun onDone(utteranceId: String?) {
                        lineOf(utteranceId)?.takeIf { it == lines.lastIndex }?.let { onProgress(lines.size, 0) }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {}
                })
            }
            lines.forEachIndexed { i, (text, pitch) ->
                e.setPitch(pitch)
                e.speak(text, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "$batch:$i")
            }
            e.setPitch(1f)
        }
    }

    /** Speaks [text] and suspends until that utterance finishes — for game loops that need accurate pacing regardless of how long a given word takes to say in a given language. */
    suspend fun speakAndAwait(text: String, locale: Locale, rate: Float) {
        val expectedId = UUID.randomUUID().toString()
        suspendCancellableCoroutine<Unit> { continuation ->
            whenReady {
                val e = engine
                if (e == null) {
                    if (continuation.isActive) continuation.resume(Unit)
                    return@whenReady
                }
                e.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == expectedId && continuation.isActive) continuation.resume(Unit)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (utteranceId == expectedId && continuation.isActive) continuation.resume(Unit)
                    }
                })
                e.language = locale
                e.setSpeechRate(rate)
                e.stop()
                e.speak(text, TextToSpeech.QUEUE_FLUSH, null, expectedId)
            }
        }
    }

    /** Cuts off whatever is being spoken — e.g. before the mic opens, so the voice isn't recorded. */
    fun stop() {
        engine?.stop()
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
    }

    companion object {
        /** Opens the system's "install TTS voice data" flow, scoped to [locale] when possible. */
        fun installVoiceDataIntent(): Intent =
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
    }
}
