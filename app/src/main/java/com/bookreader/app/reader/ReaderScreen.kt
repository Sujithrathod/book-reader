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
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val narrator = context.app.narrator
    val settings by settingsFlow.collectAsState(initial = session.initialSettings)
    val narration by narrator.state.collectAsState()
    val colors = settings.theme.colors()
    val dark = settings.theme == ReadingTheme.DARK
    val isPdf = session.format == BookFormat.PDF
    var controls by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var options by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(false) }
    var themeMenu by remember { mutableStateOf(false) }
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
            bars.isAppearanceLightStatusBars = !dark && !controls && !options
            bars.isAppearanceLightNavigationBars = !dark
        }
        onDispose { }
    }

    val speed by rememberUpdatedState(settings.autoScrollSpeed)
    LaunchedEffect(autoScroll, host.nav) {
        val nav = host.nav
        if (!autoScroll || nav == null) return@LaunchedEffect
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

            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    autoScroll -> AutoScrollBar(
                        speed = settings.autoScrollSpeed,
                        onSpeed = { onSettingsChange(settings.copy(autoScrollSpeed = it.coerceIn(ReaderSettings.AUTO_SCROLL_RANGE))) },
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
                        if (pageView && settings.thumbnails && nav != null) ThumbnailStrip(host.position, nav)
                        PageSliderRow(host.position, nav)
                        ReaderToolbar(
                            speaking = shown.playState == PlayState.PLAYING,
                            onRotate = {
                                val next = ScreenOrientation.entries[(settings.orientation.ordinal + 1) % ScreenOrientation.entries.size]
                                onSettingsChange(settings.copy(orientation = next))
                                scope.launch { snackbar.showSnackbar("Screen: ${next.label}") }
                            },
                            dayNightMenu = { open, dismiss ->
                                DayNightMenu(
                                    expanded = open,
                                    onDismiss = dismiss,
                                    title = "Day/Night Mode",
                                    items = listOf(
                                        "Day Theme" to { onSettingsChange(settings.copy(theme = ReadingTheme.LIGHT)) },
                                        "Night Theme" to { onSettingsChange(settings.copy(theme = ReadingTheme.DARK)) },
                                        "Load from theme" to { themeMenu = true },
                                    ),
                                )
                                DayNightMenu(
                                    expanded = themeMenu,
                                    onDismiss = { themeMenu = false },
                                    title = "Themes",
                                    items = ReadingTheme.entries.map { t ->
                                        (if (t == settings.theme) "✓  ${t.label}" else "     ${t.label}") to { onSettingsChange(settings.copy(theme = t)) }
                                    },
                                )
                            },
                            onSpeak = { play() },
                            onAutoScroll = { controls = false; autoScroll = true },
                            onContents = { open("toc") },
                            onMore = { open("look") },
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
                    onBack = onBack,
                    onReflow = { session.pdfReflow = !session.pdfReflow },
                ) { dismiss ->
                    MenuEntry(if (isPdf) "PDF options" else "Reading options") { dismiss(); controls = false; options = true }
                    MenuEntry("Reading settings") { dismiss(); open("look") }
                    MenuEntry("Voice and speed") { dismiss(); open("voices") }
                    MenuEntry("Sleep timer") { dismiss(); open("timer") }
                    if (isPdf && !settings.reflowButton) {
                        MenuEntry(if (session.pdfReflow) "Show pages" else "Text reflow") { dismiss(); session.pdfReflow = !session.pdfReflow }
                    }
                }
            }

            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = if (controls) 200.dp else 72.dp))

            if (options) ReaderOptionsScreen(isPdf, settings, onSettingsChange) { options = false }
        }

        if (sheet != null) {
            ModalBottomSheet(onDismissRequest = { sheet = null }) {
                when (sheet) {
                    "toc" -> TocList(host.toc) { entry -> sheet = null; entry.onSelect() }
                    "look" -> SettingsPanel(settings, session.format, onSettingsChange) { sheet = null; options = true }
                    "voices" -> VoicePanel(narrator, settings, onSettingsChange)
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

@Composable
private fun VoicePanel(narrator: Narrator, settings: ReaderSettings, onChange: (ReaderSettings) -> Unit) {
    val context = LocalContext.current
    val engine by narrator.engine.collectAsState()
    val voices by narrator.voices.collectAsState()
    val v = settings.voice
    val selected = narrator.effectiveVoice()?.name
    fun update(newVoice: VoiceSettings) = onChange(settings.copy(voice = newVoice))
    fun launch(intent: android.content.Intent) {
        try { context.startActivity(intent) } catch (e: ActivityNotFoundException) { /* no settings screen on this phone */ }
    }

    LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Voice and speed", style = MaterialTheme.typography.titleLarge) }

        item { Text("Speed: ${"%.1f".format(v.rate)}×", style = MaterialTheme.typography.labelLarge) }
        item {
            ThinSlider(value = v.rate, onValueChange = { update(v.copy(rate = it)) }, valueRange = VoiceSettings.RATE_RANGE, steps = 19)
        }
        item { Text("Pitch: ${"%.1f".format(v.pitch)}", style = MaterialTheme.typography.labelLarge) }
        item {
            ThinSlider(value = v.pitch, onValueChange = { update(v.copy(pitch = it)) }, valueRange = VoiceSettings.PITCH_RANGE, steps = 14)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Long-press a word to read at once")
                    Text(
                        "Off: long-press shows a menu with \"Read from here\" and \"Copy\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = settings.longPressReads, onCheckedChange = { onChange(settings.copy(longPressReads = it)) })
            }
        }
        item { HorizontalDivider() }

        when (engine) {
            EngineStatus.NOT_STARTED, EngineStatus.STARTING -> item { Text("Loading the phone's voices…") }
            EngineStatus.UNAVAILABLE -> item {
                Column {
                    Text("No text-to-speech engine is installed on this phone. Install \"Speech Services by Google\" from the Play Store, then come back.")
                    TextButton(onClick = { launch(Narrator.ttsSettingsIntent()) }) { Text("Open text-to-speech settings") }
                }
            }
            EngineStatus.READY -> {
                if (voices.isEmpty()) {
                    item { Text("This phone reports no voices. Download one in the text-to-speech settings.") }
                }
                VoiceChoice.grouped(voices).forEach { (language, list) ->
                    item { Text(language, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp)) }
                    list.forEach { voice ->
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                RadioButton(
                                    selected = voice.name == selected,
                                    onClick = { update(v.copy(voiceName = voice.name)) },
                                    enabled = voice.usable,
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(voice.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    val tags = listOfNotNull(
                                        if (voice.notInstalled) "Not downloaded" else null,
                                        if (voice.needsInternet) "Needs internet" else "Works offline",
                                    )
                                    Text(tags.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = { narrator.preview(voice.name) }, enabled = voice.usable) { Text("Preview") }
                            }
                        }
                    }
                }
                item {
                    Column {
                        Text(
                            "Want more voices or languages? Download them in Android's text-to-speech settings. Voices marked \"Works offline\" read without internet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Row {
                            TextButton(onClick = { launch(Narrator.installVoicesIntent()) }) { Text("Get more voices") }
                            TextButton(onClick = { narrator.refreshVoices() }) { Text("Refresh list") }
                        }
                    }
                }
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
    val nightImages = settings.theme == ReadingTheme.DARK && settings.darkImages == DarkPictures.INVERT
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
            autoScroll = { speed ->
                while (true) {
                    delay((11 - speed()) * 1_000L)
                    if (!nav.goForward(animated = true)) break
                }
            },
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

@Composable
private fun TocList(entries: List<TocEntry>, onSelect: (TocEntry) -> Unit) {
    Text("Contents", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
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

@Composable
private fun SettingsPanel(s: ReaderSettings, format: BookFormat, onChange: (ReaderSettings) -> Unit, onMoreOptions: () -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Reading settings", style = MaterialTheme.typography.titleLarge)

        Text("Theme", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadingTheme.entries.forEach { t ->
                FilterChip(selected = s.theme == t, onClick = { onChange(s.copy(theme = t)) }, label = { Text(t.label) })
            }
        }

        if (s.theme == ReadingTheme.DARK && format != BookFormat.TXT) {
            val isPdf = format == BookFormat.PDF
            Text(if (isPdf) "Pages in dark mode" else "Pictures in dark mode", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkPictures.entries.forEach { mode ->
                    val selected = if (isPdf) s.darkPdfPages == mode else s.darkImages == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onChange(if (isPdf) s.copy(darkPdfPages = mode) else s.copy(darkImages = mode)) },
                        label = { Text(mode.label) },
                    )
                }
            }
        }

        if (format == BookFormat.PDF) {
            Text(
                "PDF pages keep their original layout. Text size, spacing and margins apply in text reflow (the \"A\" button).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text("Text size", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onChange(s.copy(fontScale = (s.fontScale - 0.1).coerceIn(ReaderSettings.FONT_RANGE))) }) { Text("A−") }
            Text("${(s.fontScale * 100).toInt()}%", modifier = Modifier.padding(horizontal = 16.dp))
            OutlinedButton(onClick = { onChange(s.copy(fontScale = (s.fontScale + 0.1).coerceIn(ReaderSettings.FONT_RANGE))) }) { Text("A+") }
        }

        Text("Line spacing", style = MaterialTheme.typography.labelLarge)
        ThinSlider(
            value = s.lineHeight.toFloat(),
            onValueChange = { onChange(s.copy(lineHeight = it.toDouble())) },
            valueRange = ReaderSettings.LINE_RANGE.start.toFloat()..ReaderSettings.LINE_RANGE.endInclusive.toFloat(),
            steps = 9,
        )

        Text("Margins", style = MaterialTheme.typography.labelLarge)
        ThinSlider(
            value = s.pageMargins.toFloat(),
            onValueChange = { onChange(s.copy(pageMargins = it.toDouble())) },
            valueRange = ReaderSettings.MARGIN_RANGE.start.toFloat()..ReaderSettings.MARGIN_RANGE.endInclusive.toFloat(),
            steps = 5,
        )

        if (format == BookFormat.EPUB) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Scroll instead of turning pages", modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Switch(checked = s.scroll, onCheckedChange = { onChange(s.copy(scroll = it)) }, colors = chromeSwitchColors())
            }
        }
        TextButton(onClick = onMoreOptions) { Text(if (format == BookFormat.PDF) "More PDF options" else "More options") }
    }
}
