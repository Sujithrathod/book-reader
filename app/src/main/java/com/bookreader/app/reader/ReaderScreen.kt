package com.bookreader.app.reader

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Color
import com.bookreader.app.data.BookmarkEntity
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.bookreader.app.narration.VoiceInfo
import java.util.Locale
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.compose.AndroidFragment
import com.bookreader.app.BookFormat
import com.bookreader.app.app
import com.bookreader.app.data.DarkPictures
import com.bookreader.app.data.PageFlip
import com.bookreader.app.data.ReaderSettings
import com.bookreader.app.data.ReadingTheme
import com.bookreader.app.data.ScreenOrientation
import com.bookreader.app.narration.BlockPosition
import com.bookreader.app.narration.EngineStatus
import com.bookreader.app.narration.EpubSource
import com.bookreader.app.narration.NarrationSource
import com.bookreader.app.narration.NarrationState
import com.bookreader.app.narration.Narrator
import com.bookreader.app.narration.PlayState
import com.bookreader.app.narration.VoiceChoice
import com.bookreader.app.narration.VoiceSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.positions

/** One row in the contents list: a chapter (EPUB, TXT, PDF outline) or a page (PDF without outline). */
data class TocEntry(val title: String, val depth: Int, val onSelect: () -> Unit)

/**
 * How each format starts reading aloud.
 * [playFromScreen] starts at the top of what's on screen; [resumeListening] restarts at a
 * saved listening position and returns false if that position can't be used.
 */
class NarrationHooks(
    val playFromScreen: () -> Unit,
    val resumeListening: (String) -> Boolean,
)

/**
 * The reading screen, laid out like Moon+ Reader: the page fills the screen with a small
 * status line under it (battery, time, chapter, page). Tapping the middle of the page shows
 * the grey top bar and, at the bottom, a page slider and six tools (rotation, day/night,
 * read aloud, auto scroll, contents, more). Tapping the edges turns pages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    session: ReaderSession,
    settingsFlow: Flow<ReaderSettings>,
    onSettingsChange: (ReaderSettings) -> Unit,
    onSavePosition: (location: String, progress: Double) -> Unit,
    onStartNarration: (NarrationSource) -> Unit,
    /** Gives the activity what the volume buttons should do (+1 next, -1 previous), or null for their normal job. */
    onVolumeKeys: (((Int) -> Unit)?) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val narrator = context.app.narrator
    val settings by settingsFlow.collectAsState(initial = session.initialSettings)
    val narration by narrator.state.collectAsState()
    val colors = settings.theme.colors()
    val dark = settings.theme.dark
    val isPdf = session.format == BookFormat.PDF
    var controls by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var options by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(false) }
    var autoPaused by remember { mutableStateOf(false) }
    val bookmarkDao = remember { context.app.database.bookmarks() }
    val bookmarks by remember(session) { bookmarkDao.observe(session.book.id) }.collectAsState(initial = emptyList())
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val host = remember(session) { ReaderHost(save = onSavePosition, startNarration = onStartNarration) }

    val mine = narration.bookId == session.book.id && narration.playState != PlayState.IDLE
    val shown = if (narration.bookId == session.book.id) narration else NarrationState()

    LaunchedEffect(settings) {
        session.currentSettings = settings
        narrator.applySettings(settings.voice)
    }
    LaunchedEffect(Unit) { narrator.ensureEngine() }
    LaunchedEffect(shown.block, shown.sentence) {
        val b = shown.block
        val s = shown.sentence
        if (b != null && s != null) session.listenLocation = b.position.encode(s.start)
    }
    LaunchedEffect(narration.message) {
        if (narration.bookId == session.book.id) narration.message?.let { snackbar.showSnackbar(it) }
    }

    host.notify = { message -> scope.launch { snackbar.showSnackbar(message) } }
    host.onTap = { zone ->
        when {
            autoScroll -> autoScroll = false
            controls -> controls = false
            zone == TapZone.MENU -> controls = true
            zone == TapZone.PREVIOUS -> host.nav?.turn(-1)
            else -> host.nav?.turn(+1)
        }
    }

    // Full screen while reading: the navigation bar shows only with the menu.
    val view = LocalView.current
    DisposableEffect(controls, options, dark) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val bars = WindowCompat.getInsetsController(window, view)
            bars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (controls || options) bars.show(WindowInsetsCompat.Type.navigationBars())
            else bars.hide(WindowInsetsCompat.Type.navigationBars())
            bars.isAppearanceLightStatusBars = !dark
            bars.isAppearanceLightNavigationBars = !dark
        }
        onDispose { }
    }

    val speed by rememberUpdatedState(settings.autoScrollSpeed)
    LaunchedEffect(autoScroll) { if (!autoScroll) autoPaused = false }
    LaunchedEffect(autoScroll, autoPaused, host.nav) {
        val nav = host.nav
        if (!autoScroll || autoPaused || nav == null) return@LaunchedEffect
        try {
            nav.autoScroll { speed }
        } catch (e: CancellationException) {
            // A finger on the page stops auto scroll; leaving the screen cancels this effect.
            if (!currentCoroutineContext().isActive) throw e
        }
        autoScroll = false
    }

    fun play() {
        if (mine) { narrator.togglePlayPause(); return }
        val h = host.hooks ?: return
        val saved = session.listenLocation
        if (saved == null || !h.resumeListening(saved)) h.playFromScreen()
    }

    fun open(panel: String) { controls = false; sheet = panel }

    // Volume buttons turn pages when that setting is on, except while this book is read aloud.
    val volumeTurns = settings.volumeKeysTurn && !mine && !options && sheet == null
    DisposableEffect(volumeTurns) {
        onVolumeKeys(if (volumeTurns) { d -> host.nav?.turn(d) } else null)
        onDispose { onVolumeKeys(null) }
    }

    val here = host.position
    val bookmarked = bookmarks.any { it.page == here.page }
    fun toggleBookmark() {
        scope.launch {
            if (bookmarked) {
                bookmarkDao.deleteAt(session.book.id, here.page)
                snackbar.showSnackbar("Bookmark removed")
            } else {
                bookmarkDao.insert(BookmarkEntity(bookId = session.book.id, page = here.page, label = here.label, chapter = here.chapter, createdAt = System.currentTimeMillis()))
                snackbar.showSnackbar("Bookmark added")
            }
        }
    }
    val voices by narrator.voices.collectAsState()
    val voiceLabel = VoiceChoice.effective(voices, settings.voice.voiceName, narrator.bookLanguage, Locale.getDefault())
        ?.let { VoiceChoice.title(it, voices) } ?: "Choose a voice"

    // Back closes the menu or stops auto scroll before it leaves the book.
    BackHandler(enabled = controls || autoScroll) { controls = false; autoScroll = false }

    ChromeTheme(dark = dark) {
        Box(Modifier.fillMaxSize().background(colors.background)) {
            Column(Modifier.fillMaxSize()) {
                Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (session.format) {
                        BookFormat.EPUB -> EpubReader(session, settings, shown, host)
                        BookFormat.PDF -> PdfReader(session, settings, shown, host)
                        BookFormat.TXT -> TxtReader(session, settings, shown, host)
                    }
                }
                if (settings.statusLine) StatusLine(host.position, colors.text.copy(alpha = 0.6f))
            }
            // Warm light: an orange tint over the page that cuts blue light. It doesn't take touches.
            if (settings.warmth > 0f) Box(Modifier.fillMaxSize().background(Color(0xFFFF8A00).copy(alpha = settings.warmth * 0.3f)))

            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    autoScroll -> AutoScrollBar(
                        speed = settings.autoScrollSpeed,
                        paused = autoPaused,
                        progress = host.autoTurn,
                        onSpeed = { onSettingsChange(settings.copy(autoScrollSpeed = it.coerceIn(ReaderSettings.AUTO_SCROLL_RANGE))) },
                        onPause = { autoPaused = !autoPaused },
                        onStop = { autoScroll = false },
                    )
                    mine -> TtsBar(
                        playing = shown.playState == PlayState.PLAYING || shown.playState == PlayState.LOADING,
                        timerOn = shown.sleepRemainingMs != null,
                        onPlayPause = { narrator.togglePlayPause() },
                        onPrevious = { narrator.previousSentence() },
                        onNext = { narrator.nextSentence() },
                        onStop = { narrator.stop() },
                        onTimer = { open("timer") },
                        onVoices = { open("voices") },
                    )
                }
                if (!controls && settings.statusLine && (autoScroll || mine)) Spacer(Modifier.height(28.dp))
                AnimatedVisibility(controls, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                    Column {
                        val pageView = isPdf && !session.pdfReflow
                        if (pageView) {
                            PdfFloatingButtons(
                                paged = settings.pdfPaged,
                                cropped = settings.pdfCropMargins,
                                onTogglePaged = { onSettingsChange(settings.copy(pdfPaged = !settings.pdfPaged)) },
                                onToggleCrop = { onSettingsChange(settings.copy(pdfCropMargins = !settings.pdfCropMargins)) },
                            )
                        }
                        val nav = host.nav
                        ReaderBottomPanel(
                            position = host.position,
                            nav = nav,
                            speaking = shown.playState == PlayState.PLAYING,
                            theme = settings.theme,
                            onTheme = { onSettingsChange(settings.copy(theme = it)) },
                            onContents = { open("toc") },
                            onSpeak = { play() },
                            onAutoScroll = { controls = false; autoScroll = true },
                            onSettings = { controls = false; options = true },
                            top = { if (pageView && settings.thumbnails && nav != null) ThumbnailStrip(host.position, nav) },
                        )
                    }
                }
            }

            AnimatedVisibility(
                controls,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
            ) {
                ReaderTopBar(
                    title = session.book.title,
                    showReflow = isPdf && settings.reflowButton,
                    reflowOn = session.pdfReflow,
                    bookmarked = bookmarked,
                    onBookmark = { toggleBookmark() },
                    onBack = onBack,
                    onReflow = { session.pdfReflow = !session.pdfReflow },
                ) { dismiss ->
                    MenuEntry("Settings") { dismiss(); controls = false; options = true }
                    MenuEntry("Voice and speed") { dismiss(); open("voices") }
                    MenuEntry("Sleep timer") { dismiss(); open("timer") }
                    if (isPdf && !settings.reflowButton) {
                        MenuEntry(if (session.pdfReflow) "Show pages" else "Text reflow") { dismiss(); session.pdfReflow = !session.pdfReflow }
                    }
                }
            }

            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = if (controls) 230.dp else 72.dp))

            if (options) {
                ReaderSettingsScreen(
                    format = session.format,
                    s = settings,
                    onChange = onSettingsChange,
                    voiceLabel = voiceLabel,
                    timerLabel = shown.sleepRemainingMs?.let { "${(it / 60_000) + 1} min left" } ?: "Off",
                    onVoices = { sheet = "voices" },
                    onTimer = { sheet = "timer" },
                    onClose = { options = false },
                )
            }
        }

        if (sheet != null) {
            ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = Chrome.panel) {
                when (sheet) {
                    "toc" -> ContentsSheet(
                        entries = host.toc,
                        bookmarks = bookmarks,
                        onSelect = { entry -> sheet = null; entry.onSelect() },
                        onBookmark = { b -> sheet = null; host.nav?.goToPage(b.page) },
                        onDelete = { b -> scope.launch { bookmarkDao.delete(b.id) } },
                    )
                    "voices" -> VoicePanel(narrator, settings, null, onSettingsChange, host.notify)
                    "timer" -> TimerPanel(shown.sleepRemainingMs) { minutes -> narrator.setSleepTimer(minutes); sheet = null }
                }
            }
        }
    }
}

@Composable
private fun MenuEntry(label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label, color = Chrome.text) }, onClick = onClick)
}

@Composable
private fun TimerPanel(remainingMs: Long?, onPick: (Int?) -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Sleep timer", style = MaterialTheme.typography.titleLarge)
        Text(
            remainingMs?.let { "Reading pauses in ${(it / 60_000) + 1} min." } ?: "Reading aloud pauses when the timer ends.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 15, 30, 45, 60).forEach { m -> AssistChip(onClick = { onPick(m) }, label = { Text("$m min") }) }
        }
        if (remainingMs != null) TextButton(onClick = { onPick(null) }) { Text("Turn off timer") }
    }
}

// ---------- Voice picker ----------

/**
 * Speed, pitch and the voice list. Voices that aren't on the phone yet have a Download
 * button; the voice engine fetches them and the voice is chosen once it's ready.
 */
@Composable
private fun VoicePanel(
    narrator: Narrator,
    settings: ReaderSettings,
    bookLanguage: String?,
    onChange: (ReaderSettings) -> Unit,
    notify: (String) -> Unit,
) {
    val context = LocalContext.current
    val engine by narrator.engine.collectAsState()
    val voices by narrator.voices.collectAsState()
    val downloading by narrator.downloading.collectAsState()
    val current by rememberUpdatedState(settings)
    val v = settings.voice
    val language = narrator.bookLanguage ?: bookLanguage
    // Worked out from the saved choice itself, so the mark moves as soon as a voice is tapped.
    val selected = VoiceChoice.effective(voices, v.voiceName, language, Locale.getDefault())?.name
    var allLanguages by remember { mutableStateOf(false) }
    fun update(newVoice: VoiceSettings) = onChange(current.copy(voice = newVoice))
    fun choose(name: String) = update(current.voice.copy(voiceName = name))
    fun download(name: String) {
        notify("Downloading the voice…")
        narrator.downloadVoice(name) { ready ->
            if (ready) { choose(name); notify("Voice downloaded and selected.") }
            else notify("The voice couldn't be downloaded. Check the internet connection and try again.")
        }
    }
    fun launch(intent: android.content.Intent) {
        try { context.startActivity(intent) } catch (e: ActivityNotFoundException) { /* no settings screen on this phone */ }
    }

    // Voices downloaded elsewhere show up when the list opens or the app comes back to the front.
    LaunchedEffect(engine) { narrator.refreshVoices() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { narrator.refreshVoices() }

    LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
        item { Text("Voice and speed", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp)) }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Chrome.card).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row { Text("Speed", Modifier.weight(1f)); Text("%.1f×".format(v.rate), color = Chrome.textDim) }
                ThinSlider(value = v.rate, onValueChange = { update(current.voice.copy(rate = it)) }, valueRange = VoiceSettings.RATE_RANGE, steps = 19)
                Row { Text("Pitch", Modifier.weight(1f)); Text("%.1f".format(v.pitch), color = Chrome.textDim) }
                ThinSlider(value = v.pitch, onValueChange = { update(current.voice.copy(pitch = it)) }, valueRange = VoiceSettings.PITCH_RANGE, steps = 14)
            }
        }


        when (engine) {
            EngineStatus.NOT_STARTED, EngineStatus.STARTING -> item { Text("Loading the phone's voices…", modifier = Modifier.padding(vertical = 16.dp)) }
            EngineStatus.UNAVAILABLE -> item {
                Column(Modifier.padding(vertical = 16.dp)) {
                    Text("No text-to-speech engine is installed on this phone. Install \"Speech Services by Google\" from the Play Store, then come back.")
                    TextButton(onClick = { launch(Narrator.ttsSettingsIntent()) }) { Text("Open text-to-speech settings") }
                }
            }
            EngineStatus.READY -> {
                if (voices.isEmpty()) {
                    item { Text("This phone reports no voices yet.", modifier = Modifier.padding(vertical = 16.dp)) }
                }
                val (groups, likely) = VoiceChoice.ordered(voices, v.voiceName, language, Locale.getDefault())
                val shown = if (allLanguages) groups else groups.take(likely.coerceAtLeast(1))
                shown.forEach { (languageName, list) ->
                    item(key = "lang:$languageName") {
                        Text(languageName, color = Chrome.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp))
                    }
                    item(key = "voices:$languageName") {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Chrome.card)) {
                            list.forEachIndexed { i, voice ->
                                if (i > 0) HorizontalDivider(color = Chrome.divider, modifier = Modifier.padding(horizontal = 16.dp))
                                VoiceRow(
                                    title = "Voice ${i + 1}",
                                    voice = voice,
                                    selected = voice.name == selected,
                                    downloadStarted = downloading[voice.name],
                                    onChoose = { if (voice.usable) choose(voice.name) else download(voice.name) },
                                    onPreview = { narrator.preview(voice.name) },
                                )
                            }
                        }
                    }
                }
                if (groups.size > shown.size || allLanguages) {
                    item {
                        TextButton(onClick = { allLanguages = !allLanguages }, modifier = Modifier.padding(top = 8.dp)) {
                            Text(if (allLanguages) "Show fewer languages" else "Show all ${groups.size} languages")
                        }
                    }
                }
                item {
                    Text(
                        "Voices marked \"Offline\" read without internet. If a download doesn't start, the phone's own voice settings can be used instead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp),
                    )
                    TextButton(onClick = { launch(Narrator.installVoicesIntent()) }) { Text("Open the phone's voice settings") }
                }
            }
        }
    }
}

@Composable
private fun VoiceRow(
    title: String,
    voice: VoiceInfo,
    selected: Boolean,
    /** When this voice's download started, or null if it isn't downloading. */
    downloadStarted: Long?,
    onChoose: () -> Unit,
    onPreview: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = selected, enabled = downloadStarted == null, role = Role.RadioButton, onClick = onChoose).padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = voice.usable, modifier = Modifier.padding(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            Text(VoiceChoice.tags(voice).joinToString(" · "), fontSize = 13.sp, color = Chrome.textDim)
        }
        when {
            downloadStarted != null -> {
                // The phone's voice engine doesn't say how far a download is, so this shows time, not percent.
                val seconds by produceState(0L, downloadStarted) {
                    while (true) { value = (System.currentTimeMillis() - downloadStarted) / 1000; delay(1000) }
                }
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(128.dp)) {
                    Text("Downloading  %d:%02d".format(seconds / 60, seconds % 60), fontSize = 13.sp, color = Chrome.textDim)
                    LinearProgressIndicator(Modifier.padding(top = 6.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = Chrome.accent, trackColor = Chrome.button)
                }
            }
            voice.usable -> IconButton(onClick = onPreview) { Icon(Icons.Rounded.PlayArrow, "Play a sample of $title", tint = Chrome.accent) }
            else -> FilledTonalButton(onClick = onChoose) {
                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                Text("Download", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

// ---------- EPUB ----------

@Composable
private fun EpubReader(session: ReaderSession, settings: ReaderSettings, narration: NarrationState, host: ReaderHost) {
    val publication = session.publication ?: return
    var navigator by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
    val scope = rememberCoroutineScope()
    val highlightTint = MaterialTheme.colorScheme.secondary
    val positions by produceState(emptyList<Locator>(), publication) {
        value = runCatching { publication.positions() }.getOrDefault(emptyList())
    }
    val toc = remember(publication) { flatten(publication.tableOfContents, 0) }
    val edgesTurn by rememberUpdatedState(settings.tapEdgesTurn)
    val animate by rememberUpdatedState(settings.pageFlip == PageFlip.SLIDE)

    AndroidFragment<EpubNavigatorFragment>(Modifier.fillMaxSize()) { navigator = it }

    LaunchedEffect(navigator, settings) {
        navigator?.submitPreferences(settings.toEpubPreferences())
    }
    // Night pictures are swapped per chapter, so redo it whenever another chapter comes on screen.
    val nightImages = settings.theme.dark && settings.darkImages == DarkPictures.INVERT
    LaunchedEffect(navigator, nightImages) {
        val nav = navigator ?: return@LaunchedEffect
        nav.currentLocator.map { it.href }.distinctUntilChanged().collectLatest {
            delay(300) // let the chapter's page finish loading
            runCatching { NightImages.sync(nav, publication, nightImages) }
        }
    }
    // Taps on the page: middle opens the menu, edges turn pages (Moon+ Reader's layout).
    DisposableEffect(navigator) {
        val nav = navigator ?: return@DisposableEffect onDispose { }
        val listener = object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                val v = nav.view ?: return false
                host.onTap(tapZone(event.point.x, event.point.y, v.width.toFloat(), v.height.toFloat(), edgesTurn))
                return true
            }
        }
        nav.addInputListener(listener)
        onDispose { nav.removeInputListener(listener) }
    }
    LaunchedEffect(navigator, positions) {
        val nav = navigator ?: return@LaunchedEffect
        host.toc = toc.map { (link, depth) ->
            TocEntry(link.title ?: "Untitled section", depth) { nav.go(link, animated = false) }
        }
        host.nav = ReaderNav(
            goToPage = { i -> positions.getOrNull(i)?.let { nav.go(it, animated = false) } },
            turn = { d -> if (d > 0) nav.goForward(animate) else nav.goBackward(animate) },
            autoScroll = { speed -> autoTurnPages(host, speed) { nav.goForward(animated = true) } },
        )
        host.hooks = NarrationHooks(
            playFromScreen = {
                scope.launch {
                    val start = nav.firstVisibleElementLocator() ?: nav.currentLocator.value
                    host.startNarration(EpubSource(publication, start, null, null))
                }
            },
            resumeListening = { saved ->
                val json = runCatching { JSONObject(saved) }.getOrNull()
                val locator = json?.optJSONObject("locator")?.let { Locator.fromJSON(it) }
                if (json == null || locator == null) false else {
                    host.startNarration(EpubSource(publication, locator, null, null, json.optInt("offset", 0)))
                    true
                }
            },
        )
        nav.currentLocator.collect { locator ->
            val progression = locator.locations.totalProgression ?: 0.0
            host.save(locator.toJSON().toString(), progression)
            val count = positions.size
            val page = ((locator.locations.position ?: 1) - 1).coerceAtLeast(0)
            val chapter = locator.title ?: toc.lastOrNull { (link, _) ->
                link.href.toString().substringBefore('#') == locator.href.toString().substringBefore('#')
            }?.first?.title
            host.position = ReaderPosition(
                page = page,
                pageCount = count,
                chapter = chapter,
                label = if (count > 0) "${page + 1}/$count" else "${(progression * 100).toInt()}%",
            )
        }
    }

    // Highlight the sentence being read and keep it on screen.
    LaunchedEffect(navigator, narration.block, narration.sentence) {
        val nav = navigator ?: return@LaunchedEffect
        val block = narration.block
        val sentence = narration.sentence
        val pos = block?.position as? BlockPosition.Epub
        if (block == null || sentence == null || pos == null) {
            nav.applyDecorations(emptyList(), "tts")
            return@LaunchedEffect
        }
        val text = block.text
        val locator = pos.locator.copy(
            text = Locator.Text(
                before = text.substring(0, sentence.start).takeLast(50),
                highlight = text.substring(sentence.start, sentence.end),
                after = text.substring(sentence.end).take(50),
            )
        )
        nav.applyDecorations(
            listOf(Decoration(id = "tts", locator = locator, style = Decoration.Style.Highlight(tint = highlightTint.toArgb()))),
            group = "tts",
        )
        nav.go(locator, animated = true)
    }
}

private fun flatten(links: List<Link>, depth: Int): List<Pair<Link, Int>> =
    links.flatMap { listOf(it to depth) + flatten(it.children, depth + 1) }

// ---------- Sheets ----------

/** The contents sheet: the book's chapters, and the reader's own bookmarks on a second tab. */
@Composable
private fun ContentsSheet(
    entries: List<TocEntry>,
    bookmarks: List<BookmarkEntity>,
    onSelect: (TocEntry) -> Unit,
    onBookmark: (BookmarkEntity) -> Unit,
    onDelete: (BookmarkEntity) -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    Row(
        Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Chrome.button).padding(3.dp),
    ) {
        listOf("Contents", if (bookmarks.isEmpty()) "Bookmarks" else "Bookmarks (${bookmarks.size})").forEachIndexed { i, name ->
            val on = tab == i
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(if (on) Chrome.accent else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab) { tab = i }.padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(name, color = if (on) Chrome.onAccent else Chrome.text, fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
            }
        }
    }
    if (tab == 0) TocList(entries, onSelect) else BookmarkList(bookmarks, onBookmark, onDelete)
}

@Composable
private fun BookmarkList(bookmarks: List<BookmarkEntity>, onOpen: (BookmarkEntity) -> Unit, onDelete: (BookmarkEntity) -> Unit) {
    if (bookmarks.isEmpty()) {
        Text(
            "No bookmarks yet. Open the menu and tap the bookmark at the top to mark a page.",
            modifier = Modifier.padding(24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val dates = remember { java.text.SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    LazyColumn(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        itemsIndexed(bookmarks, key = { _, b -> b.id }) { _, b ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(b) }.padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bookmark, null, tint = Chrome.accent, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    val chapter = b.chapter?.takeIf { it.isNotBlank() }
                    val place = if ('/' in b.label) "Page ${b.label.substringBefore('/')} of ${b.label.substringAfter('/')}" else "At ${b.label}"
                    val added = dates.format(java.util.Date(b.createdAt))
                    Text(chapter ?: place, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text(if (chapter != null) "$place · $added" else "Added $added", fontSize = 13.sp, color = Chrome.textDim)
                }
                IconButton(onClick = { onDelete(b) }) { Icon(Icons.Rounded.Close, "Remove this bookmark", tint = Chrome.textDim) }
            }
            HorizontalDivider(color = Chrome.divider, modifier = Modifier.padding(horizontal = 24.dp))
        }
    }
}

@Composable
private fun TocList(entries: List<TocEntry>, onSelect: (TocEntry) -> Unit) {
    if (entries.isEmpty()) {
        Text(
            "This book has no table of contents.",
            modifier = Modifier.padding(24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyColumn(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        itemsIndexed(entries) { _, entry ->
            Text(
                entry.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(entry) }
                    .padding(start = (24 + entry.depth * 16).dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
            )
            HorizontalDivider()
        }
    }
}
