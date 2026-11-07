package com.bookreader.app.narration

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

enum class PlayState { IDLE, LOADING, PLAYING, PAUSED }

enum class EngineStatus { NOT_STARTED, STARTING, READY, UNAVAILABLE }

/** Everything the screens need to show about narration. */
data class NarrationState(
    val playState: PlayState = PlayState.IDLE,
    val bookId: Long? = null,
    val bookTitle: String = "",
    val bookAuthor: String? = null,
    val block: TextBlock? = null,
    /** The sentence being read, as a range inside [block]. */
    val sentence: Segment? = null,
    /** The word being read, when the voice engine reports word timing. */
    val word: Segment? = null,
    val message: String? = null,
    val sleepRemainingMs: Long? = null,
)

/** Voice choices, stored with the reading settings. */
data class VoiceSettings(
    val voiceName: String? = null,
    val rate: Float = 1.0f,   // 0.5 .. 2.5
    val pitch: Float = 1.0f,  // 0.5 .. 2.0
) {
    companion object {
        val RATE_RANGE = 0.5f..2.5f
        val PITCH_RANGE = 0.5f..2.0f
    }
}

/**
 * Reads text aloud with the phone's built-in text-to-speech engine.
 *
 * Android's TextToSpeech has no pause/resume, so this class tracks its own position:
 * it speaks one sentence at a time, remembers the last word the engine reported,
 * and on resume starts again from that word.
 */
class Narrator(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audio = context.getSystemService(AudioManager::class.java)

    private val _state = MutableStateFlow(NarrationState())
    val state: StateFlow<NarrationState> = _state.asStateFlow()

    private val _engine = MutableStateFlow(EngineStatus.NOT_STARTED)
    val engine: StateFlow<EngineStatus> = _engine.asStateFlow()

    private val _voices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val voices: StateFlow<List<VoiceInfo>> = _voices.asStateFlow()

    /** Called with (bookId, encoded position) each time a new sentence starts. */
    var onListeningPosition: ((Long, String) -> Unit)? = null

    private var tts: TextToSpeech? = null
    private var settings = VoiceSettings()
    private var bookLanguage: String? = null

    // Reading position
    private var source: NarrationSource? = null
    private val history = ArrayList<TextBlock>()   // blocks read so far, for "previous sentence"
    private var blockIndex = 0
    private var sentences: List<Segment> = emptyList()
    private var sentenceIndex = 0
    private var startOffset: Int? = null            // word to start at inside the current sentence
    private var lastWordStart: Int? = null
    private var generation = 0                      // bumps on every stop, so late callbacks are ignored

    private var focusRequest: AudioFocusRequest? = null
    private var resumeOnFocusGain = false
    private var sleepJob: Job? = null
    private var voiceChangedByPreview = false

    // ---------- Engine ----------

    /** Starts the voice engine if needed. Safe to call many times. */
    fun ensureEngine() {
        if (tts != null) return
        _engine.value = EngineStatus.STARTING
        tts = TextToSpeech(context.applicationContext) { status ->
            scope.launch {
                if (status == TextToSpeech.SUCCESS) {
                    tts?.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    tts?.setOnUtteranceProgressListener(listener)
                    refreshVoices()
                    _engine.value = EngineStatus.READY
                } else {
                    _engine.value = EngineStatus.UNAVAILABLE
                    tts?.shutdown()
                    tts = null
                }
            }
        }
    }

    fun refreshVoices() {
        val list = runCatching { tts?.voices.orEmpty() }.getOrDefault(emptySet()).map { it.toInfo() }
        _voices.value = list
    }

    private fun Voice.toInfo() = VoiceInfo(
        name = name,
        locale = locale,
        needsInternet = isNetworkConnectionRequired,
        notInstalled = features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true,
        quality = quality,
    )

    fun applySettings(s: VoiceSettings) {
        val next = s.copy(
            rate = s.rate.coerceIn(VoiceSettings.RATE_RANGE),
            pitch = s.pitch.coerceIn(VoiceSettings.PITCH_RANGE),
        )
        if (next == settings) return
        settings = next
        applyVoice()
        // Speed and pitch changes take effect from the current word.
        if (_state.value.playState == PlayState.PLAYING) restartFromCurrentWord()
    }

    /** The voice that will actually be used: the chosen one if installed, else a sensible default. */
    fun effectiveVoice(): VoiceInfo? {
        val all = _voices.value
        return all.firstOrNull { it.name == settings.voiceName && it.usable }
            ?: VoiceChoice.pickDefault(all, bookLanguage, Locale.getDefault())
    }

    private fun applyVoice() {
        val engine = tts ?: return
        val target = effectiveVoice()
        if (target != null) {
            engine.voices?.firstOrNull { it.name == target.name }?.let { engine.voice = it }
        }
        voiceChangedByPreview = false
        engine.setSpeechRate(settings.rate)
        engine.setPitch(settings.pitch)
    }

    private val locale: Locale get() = effectiveVoice()?.locale ?: Locale.getDefault()

    /** Plays a short sample in [voiceName] (pauses narration first). */
    fun preview(voiceName: String) {
        val engine = tts ?: return
        if (_state.value.playState == PlayState.PLAYING) pause()
        engine.voices?.firstOrNull { it.name == voiceName }?.let { engine.voice = it }
        voiceChangedByPreview = true
        engine.setSpeechRate(settings.rate)
        engine.setPitch(settings.pitch)
        engine.speak("This is how this voice sounds when it reads your book.", TextToSpeech.QUEUE_FLUSH, Bundle(), PREVIEW_ID)
    }

    // ---------- Commands ----------

    /** Starts reading [newSource] for a book, replacing anything that was playing. */
    fun start(bookId: Long, title: String, author: String?, language: String?, newSource: NarrationSource) {
        ensureEngine()
        stopSpeech()
        source?.close()
        source = newSource
        bookLanguage = language
        history.clear()
        _state.value = NarrationState(PlayState.LOADING, bookId, title, author)
        startService()
        scope.launch {
            val first = try {
                withContext(Dispatchers.IO) { newSource.first() }
            } catch (e: Exception) {
                null
            }
            if (first == null) {
                finish(newSource.emptyMessage)
                return@launch
            }
            val (block, offset) = first
            history += block
            blockIndex = 0
            loadSentences(block)
            val wordStart = SpeechText.wordStart(block.text, offset)
            sentenceIndex = sentences.indexOfFirst { wordStart < it.end }.coerceAtLeast(0)
            startOffset = wordStart
            waitForEngineThenSpeak()
        }
    }

    fun pause() {
        val s = _state.value
        if (s.playState != PlayState.PLAYING && s.playState != PlayState.LOADING) return
        startOffset = lastWordStart ?: s.sentence?.start
        stopSpeech()
        _state.update { it.copy(playState = PlayState.PAUSED, word = null) }
        abandonFocus()
    }

    fun resume() {
        if (_state.value.playState != PlayState.PAUSED) return
        speakCurrent()
    }

    fun togglePlayPause() = if (_state.value.playState == PlayState.PLAYING) pause() else resume()

    fun stop() {
        stopSpeech()
        source?.close()
        source = null
        history.clear()
        sleepJob?.cancel()
        abandonFocus()
        _state.value = NarrationState()
    }

    fun nextSentence() = jump(+1)

    fun previousSentence() = jump(-1)

    /** null turns the timer off. */
    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null) {
            _state.update { it.copy(sleepRemainingMs = null) }
            return
        }
        val end = System.currentTimeMillis() + minutes * 60_000L
        sleepJob = scope.launch {
            while (true) {
                val left = end - System.currentTimeMillis()
                if (left <= 0) break
                _state.update { it.copy(sleepRemainingMs = left) }
                delay(1_000)
            }
            _state.update { it.copy(sleepRemainingMs = null) }
            pause()
        }
    }

    // ---------- Internals ----------

    private fun loadSentences(block: TextBlock) {
        sentences = SpeechText.sentences(block.text, locale).ifEmpty { listOf(Segment(0, block.text.length)) }
    }

    private suspend fun waitForEngineThenSpeak() {
        var waited = 0
        while (_engine.value == EngineStatus.STARTING && waited < 10_000) {
            delay(100); waited += 100
        }
        if (_engine.value != EngineStatus.READY) {
            finish("No text-to-speech voice engine is available. Install \"Speech Services by Google\" from the Play Store.")
            return
        }
        applyVoice()
        speakCurrent()
    }

    private fun speakCurrent() {
        val engine = tts ?: return
        // A voice preview switches the engine's voice; switch back before reading on.
        if (voiceChangedByPreview) applyVoice()
        val block = history.getOrNull(blockIndex) ?: return
        val sentence = sentences.getOrNull(sentenceIndex) ?: return
        val from = startOffset?.takeIf { it in sentence.start until sentence.end } ?: sentence.start
        startOffset = null
        lastWordStart = from
        if (!requestFocus()) {
            _state.update { it.copy(playState = PlayState.PAUSED, message = "Another app is using audio.") }
            startOffset = from
            return
        }
        generation++
        val id = "$generation|$from"
        _state.update {
            it.copy(playState = PlayState.PLAYING, block = block, sentence = Segment(from, sentence.end), word = null, message = null)
        }
        _state.value.bookId?.let { onListeningPosition?.invoke(it, block.position.encode(from)) }
        engine.speak(block.text.substring(from, sentence.end), TextToSpeech.QUEUE_FLUSH, Bundle(), id)
    }

    private fun restartFromCurrentWord() {
        startOffset = lastWordStart
        stopSpeech()
        speakCurrent()
    }

    private fun stopSpeech() {
        generation++
        tts?.stop()
    }

    private fun jump(delta: Int) {
        val playing = _state.value.playState == PlayState.PLAYING
        if (history.isEmpty()) return
        stopSpeech()
        scope.launch {
            var i = sentenceIndex + delta
            if (i < 0) {
                if (blockIndex > 0) {
                    blockIndex--
                    loadSentences(history[blockIndex])
                    i = sentences.lastIndex
                } else i = 0
            } else if (i > sentences.lastIndex) {
                if (!moveToNextBlock()) { finish("End of the book."); return@launch }
                i = 0
            }
            sentenceIndex = i
            startOffset = null
            lastWordStart = sentences[i].start
            if (playing) speakCurrent() else {
                _state.update { it.copy(block = history[blockIndex], sentence = sentences[i], word = null) }
            }
        }
    }

    /** Moves to the next block, reading it from the source if needed. Returns false at the end. */
    private suspend fun moveToNextBlock(): Boolean {
        if (blockIndex + 1 < history.size) {
            blockIndex++
        } else {
            val next = try {
                withContext(Dispatchers.IO) { source?.next() }
            } catch (e: Exception) {
                null
            } ?: return false
            history += next
            // Keep memory small on long books: only recent blocks are kept for "previous".
            if (history.size > 50) history.removeAt(0)
            blockIndex = history.lastIndex
        }
        loadSentences(history[blockIndex])
        return true
    }

    private fun onSentenceDone() {
        scope.launch {
            if (sentenceIndex + 1 < sentences.size) {
                sentenceIndex++
            } else {
                if (!moveToNextBlock()) { finish("End of the book."); return@launch }
                sentenceIndex = 0
            }
            speakCurrent()
        }
    }

    private fun finish(message: String) {
        stopSpeech()
        abandonFocus()
        _state.update { it.copy(playState = PlayState.IDLE, word = null, message = message) }
    }

    private val listener = object : UtteranceProgressListener() {
        private fun current(id: String?) = id != null && id != PREVIEW_ID && id.substringBefore('|') == generation.toString()

        override fun onStart(utteranceId: String?) {}

        override fun onDone(utteranceId: String?) {
            scope.launch { if (current(utteranceId)) onSentenceDone() }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            scope.launch { if (current(utteranceId)) finish("The voice engine couldn't read this sentence.") }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            scope.launch {
                if (!current(utteranceId)) return@launch
                val msg = when (errorCode) {
                    TextToSpeech.ERROR_NETWORK, TextToSpeech.ERROR_NETWORK_TIMEOUT ->
                        "This voice needs internet. Pick an offline voice or connect to the internet."
                    TextToSpeech.ERROR_NOT_INSTALLED_YET -> "This voice isn't downloaded yet. Pick another voice or install it in Android settings."
                    else -> "The voice engine couldn't read this sentence."
                }
                startOffset = lastWordStart
                stopSpeech()
                _state.update { it.copy(playState = PlayState.PAUSED, message = msg) }
            }
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            scope.launch {
                if (!current(utteranceId)) return@launch
                val base = utteranceId!!.substringAfter('|').toIntOrNull() ?: return@launch
                lastWordStart = base + start
                _state.update { it.copy(word = Segment(base + start, base + end)) }
            }
        }
    }

    // ---------- Audio focus ----------

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        scope.launch {
            when (change) {
                AudioManager.AUDIOFOCUS_LOSS -> { resumeOnFocusGain = false; pause() }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    if (_state.value.playState == PlayState.PLAYING) { resumeOnFocusGain = true; pause() }
                }
                AudioManager.AUDIOFOCUS_GAIN -> if (resumeOnFocusGain) { resumeOnFocusGain = false; resume() }
            }
        }
    }

    private fun requestFocus(): Boolean {
        val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
            .also { focusRequest = it }
        return audio.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonFocus() {
        focusRequest?.let { audio.abandonAudioFocusRequest(it) }
    }

    private fun startService() {
        runCatching { context.startForegroundService(Intent(context, NarrationService::class.java)) }
    }

    companion object {
        private const val PREVIEW_ID = "preview"

        /** Opens the screen where people download more voices. */
        fun installVoicesIntent(): Intent =
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        /** Opens Android's text-to-speech settings. */
        fun ttsSettingsIntent(): Intent =
            Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
