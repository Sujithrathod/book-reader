package com.bookreader.app.reader

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.bookreader.app.data.DarkPictures
import com.bookreader.app.data.PageFlip
import com.bookreader.app.data.ReaderSettings
import com.bookreader.app.data.ReadingTheme
import com.bookreader.app.narration.BlockPosition
import com.bookreader.app.narration.NarrationState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpOffset
import com.bookreader.app.data.ReadingFont
import com.bookreader.app.narration.PdfText
import com.bookreader.app.narration.PdfWord
import com.bookreader.app.narration.PdfWords
import com.bookreader.app.narration.PdfSource
import com.bookreader.app.narration.Segment
import com.bookreader.app.narration.TxtSource
import com.bookreader.app.text.PlainText
import com.bookreader.app.text.PlainTextBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

// ---------- Shared ----------

/** Reads "page:12" or "para:12" back into 12. */
internal fun indexFrom(location: String?, prefix: String): Int =
    location?.removePrefix("$prefix:")?.substringBefore('@')?.toIntOrNull()?.coerceAtLeast(0) ?: 0

/** Parses a listening position such as "para:12@40" into (12, 40). */
internal fun parseTxtListen(saved: String): Pair<Int, Int>? {
    if (!saved.startsWith("para:")) return null
    val para = saved.removePrefix("para:").substringBefore('@').toIntOrNull() ?: return null
    val offset = saved.substringAfter('@', "0").toIntOrNull() ?: 0
    return para to offset
}

/** Parses "pdfpage:3.2@40" into (page 3, paragraph 2, offset 40). */
internal fun parsePdfListen(saved: String): Triple<Int, Int, Int>? {
    if (!saved.startsWith("pdfpage:")) return null
    val body = saved.removePrefix("pdfpage:")
    val page = body.substringBefore('.').toIntOrNull() ?: return null
    val para = body.substringAfter('.').substringBefore('@').toIntOrNull() ?: 0
    val offset = body.substringAfter('@', "0").toIntOrNull() ?: 0
    return Triple(page, para, offset)
}

/** Publishes the item on screen to the chrome at once, and saves it once scrolling settles. */
@OptIn(FlowPreview::class)
@Composable
private fun TrackPage(key: Any, current: () -> Int, publish: (Int) -> Unit, save: (Int) -> Unit) {
    val pub by rememberUpdatedState(publish)
    val sav by rememberUpdatedState(save)
    LaunchedEffect(key) {
        snapshotFlow(current).distinctUntilChanged().onEach { pub(it) }.debounce(400).collect { sav(it) }
    }
}

/**
 * Navigation for a continuous scroll; auto scroll glides. When each item is a page (PDF),
 * a tap switches to the start of the next or previous page; otherwise (TXT paragraphs) it
 * moves about one screen.
 */
private fun listNav(
    state: LazyListState,
    scope: CoroutineScope,
    density: Float,
    animate: Boolean,
    itemsArePages: Boolean,
    thumbnail: (suspend (Int, Int) -> Bitmap?)? = null,
) = ReaderNav(
    goToPage = { i -> scope.launch { state.scrollToItem(i) } },
    turn = { d ->
        scope.launch {
            if (itemsArePages) {
                val last = (state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                val target = (state.firstVisibleItemIndex + d).coerceIn(0, last)
                if (animate) state.animateScrollToItem(target) else state.scrollToItem(target)
            } else {
                val screen = (state.layoutInfo.viewportEndOffset - state.layoutInfo.viewportStartOffset) * 0.9f
                if (animate) state.animateScrollBy(d * screen) else state.scrollBy(d * screen)
            }
        }
    },
    autoScroll = { speed ->
        // Paced by the clock, so the speed is the same on 60 Hz and 120 Hz screens.
        var last = 0L
        while (state.canScrollForward) {
            val now = withFrameNanos { it }
            if (last != 0L) {
                val seconds = ((now - last) / 1e9f).coerceAtMost(0.1f)
                state.scrollBy(autoScrollDpPerSecond(speed()) * density * seconds)
            }
            last = now
        }
    },
    thumbnail = thumbnail,
)

/** Scrolls so item [index] is visible, without moving if it already is. */
private suspend fun LazyListState.reveal(index: Int) {
    val visible = layoutInfo.visibleItemsInfo
    val fullyVisible = visible.any { it.index == index && it.offset >= 0 && it.offset + it.size <= layoutInfo.viewportEndOffset }
    if (!fullyVisible) animateScrollToItem(index)
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

// ---------- PDF ----------

private val FULL_PAGE = RectF(0f, 0f, 1f, 1f)

/** Opens a PDF with Android's built-in PdfRenderer. One renderer is shared, guarded by a lock. */
private class PdfDocument(file: File) : AutoCloseable {
    private val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(pfd)
    private val lock = Mutex()
    val pageCount = renderer.pageCount

    /** Width / height of every page, read once so the list can reserve space before rendering. */
    val aspectRatios: List<Float> = (0 until pageCount).map { i ->
        renderer.openPage(i).use { it.width.toFloat() / it.height.coerceAtLeast(1) }
    }

    /** Part of each page that has ink, as fractions of the page; found once per page. */
    private val contentBoxes = ConcurrentHashMap<Int, RectF>()

    @Volatile private var closed = false
    private var released = false // guarded by lock

    /**
     * Renders page [index] [widthPx] wide. With [crop], white borders are cut off and the
     * rest is rendered at full sharpness. With [night], the page is turned into its night
     * version (see [NightPage]). Returns null once the document is closed.
     */
    suspend fun render(index: Int, widthPx: Int, crop: Boolean, night: Boolean = false): Bitmap? = withContext(Dispatchers.IO) {
        val bitmap = lock.withLock {
            try {
                if (closed) return@withLock null
                renderer.openPage(index).use { page ->
                    val box = if (crop) contentBoxes.getOrPut(index) { contentBox(page) } else FULL_PAGE
                    val boxW = box.width() * page.width
                    val boxH = box.height() * page.height
                    val w = widthPx.coerceIn(1, 2400)
                    val scale = w / boxW
                    val h = (boxH * scale).toInt().coerceIn(1, 6000)
                    val transform = Matrix().apply {
                        setTranslate(-box.left * page.width, -box.top * page.height)
                        postScale(scale, scale)
                    }
                    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
                        it.eraseColor(AndroidColor.WHITE)
                        page.render(it, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            } finally {
                if (closed) release()
            }
        } ?: return@withContext null
        if (night) {
            val px = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            NightPage.apply(px, bitmap.width, bitmap.height)
            bitmap.setPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        }
        bitmap
    }

    /** The part of page [index] that [render] shows (fractions of the page); the whole page unless cropped. */
    fun boxOf(index: Int, crop: Boolean): RectF = if (crop) contentBoxes[index] ?: FULL_PAGE else FULL_PAGE

    /** Finds the inked part of a page from a small render, with a little padding around it. */
    private fun contentBox(page: PdfRenderer.Page): RectF {
        val w = 240
        val h = (w.toFloat() * page.height / page.width).toInt().coerceIn(1, 1200)
        val small = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        small.eraseColor(AndroidColor.WHITE)
        page.render(small, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        small.recycle()
        var minX = w; var minY = h; var maxX = -1; var maxY = -1
        for (y in 0 until h) for (x in 0 until w) {
            val c = px[y * w + x]
            val lum = (AndroidColor.red(c) * 299 + AndroidColor.green(c) * 587 + AndroidColor.blue(c) * 114) / 1000
            if (lum < 235) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
        if (maxX < 0) return FULL_PAGE
        val pad = 0.02f
        return RectF(
            (minX.toFloat() / w - pad).coerceAtLeast(0f),
            (minY.toFloat() / h - pad).coerceAtLeast(0f),
            ((maxX + 1f) / w + pad).coerceAtMost(1f),
            ((maxY + 1f) / h + pad).coerceAtMost(1f),
        )
    }

    /** Safe while a page is rendering: that render releases the file when it finishes. */
    override fun close() {
        closed = true
        if (lock.tryLock()) {
            try { release() } finally { lock.unlock() }
        }
    }

    private fun release() {
        if (released) return
        released = true
        renderer.close()
        pfd.close()
    }
}

/**
 * Dark theme look for a rendered PDF page, as a colour filter. INVERT needs no filter: the
 * page's pixels are turned into the night version when rendered (see [NightPage]), which keeps
 * dark pictures dark. DIM halves the glare instead.
 */
private fun pdfDarkFilter(mode: DarkPictures): ColorFilter? = when (mode) {
    DarkPictures.INVERT -> null
    DarkPictures.DIM -> ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.55f, 0.55f, 0.55f, 1f) })
    DarkPictures.ORIGINAL -> null
}

/** How PDF pages and the space around them look with the current theme. */
private class PdfLook(settings: ReaderSettings) {
    private val dark = settings.theme.dark
    val filter = if (dark) pdfDarkFilter(settings.darkPdfPages) else null
    /** Night page: inverted, with dark pictures kept dark. */
    val night = dark && settings.darkPdfPages == DarkPictures.INVERT
    val pageBackground = if (dark && settings.darkPdfPages == DarkPictures.INVERT) Color.Black else Color.White
    /** Margin colour applies to light themes; the dark theme is black around pages too, as in Moon+ Reader. */
    val margin = if (dark) Color.Black else Color(settings.pdfMarginColor)
}

/**
 * PDF pages, shown as images in a continuous scroll or one page at a time, or as
 * reflowed text (the "A" button) where words can be long-pressed to read aloud.
 */
@Composable
internal fun PdfReader(session: ReaderSession, settings: ReaderSettings, narration: NarrationState, host: ReaderHost) {
    val context = LocalContext.current
    val file = remember(session) { File(session.book.filePath) }
    val doc by produceState<Result<PdfDocument>?>(null, file) {
        value = withContext(Dispatchers.IO) { runCatching { PdfDocument(file) } }
    }
    // Capture the opened document: reading `doc` inside onDispose would see the newer value
    // and close the document that just opened.
    val opened = doc?.getOrNull()
    DisposableEffect(opened) { onDispose { opened?.close() } }
    val outline by produceState(emptyList<OutlineEntry>(), file) { value = PdfOutline.load(context, file) }

    val result = doc ?: return Centered { CircularProgressIndicator() }
    val pdf = result.getOrElse {
        return Centered { Text("This PDF could not be opened.", color = settings.theme.colors().text, modifier = Modifier.padding(24.dp)) }
    }
    val count = pdf.pageCount

    LaunchedEffect(pdf, outline) {
        host.toc = if (outline.isNotEmpty()) {
            outline.map { e -> TocEntry(e.title, e.depth) { host.nav?.goToPage(e.page) } }
        } else {
            (0 until count).map { i -> TocEntry("Page ${i + 1}", 0) { host.nav?.goToPage(i) } }
        }
        host.hooks = NarrationHooks(
            playFromScreen = { host.startNarration(PdfSource(context, file, host.position.page)) },
            resumeListening = resume@{ saved ->
                val (page, para, offset) = parsePdfListen(saved) ?: return@resume false
                host.startNarration(PdfSource(context, file, page, para, offset))
                true
            },
        )
        if (host.position.pageCount == count) host.position = host.position.copy(chapter = chapterAt(outline, host.position.page))
    }

    val start = (host.position.takeIf { it.pageCount == count }?.page ?: indexFrom(session.lastLocation, "page")).coerceIn(0, count - 1)
    val publish: (Int) -> Unit = { i -> host.position = ReaderPosition(i, count, chapterAt(outline, i), "${i + 1}/$count") }
    val save: (Int) -> Unit = { i -> host.save("page:$i", if (count <= 1) 1.0 else i.toDouble() / (count - 1)) }
    val readingPage = (narration.block?.position as? BlockPosition.Pdf)?.page
    val tap = rememberTapArea(host, settings.tapEdgesTurn)
    val thumbnail: suspend (Int, Int) -> Bitmap? = { page, w -> pdf.render(page, w, crop = false) }

    // Long-press on a word of the page picture: the word is found from the PDF's text positions.
    val textPages = remember(file) { PdfTextPages(context, file) }
    DisposableEffect(textPages) { onDispose { textPages.close() } }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var marked by remember { mutableStateOf<Pair<Int, PdfWord>?>(null) }
    // The words being read aloud, to highlight them on the page picture.
    val reading = narration.block?.position as? BlockPosition.Pdf
    val placed by produceState<Pair<Int, List<Pair<PdfWord, Pair<Int, Int>>>>?>(null, reading?.page) {
        val page = reading?.page
        value = if (page == null) null else {
            val words = textPages.words(page)
            val paragraphs = textPages.paragraphs(page)
            val at = withContext(Dispatchers.Default) { PdfText.locateAll(paragraphs, words.map { it.text }) }
            page to words.zip(at).mapNotNull { (w, a) -> a?.let { w to it } }
        }
    }
    fun wordsIn(segment: Segment?): List<PdfWord> {
        val p = placed
        if (segment == null || reading == null || p == null || p.first != reading.page) return emptyList()
        return p.second.filter { (_, at) -> at.first == reading.paragraph && at.second >= segment.start && at.second < segment.end }.map { it.first }
    }
    val sentenceBars = remember(placed, reading, narration.sentence) { PdfWords.lines(wordsIn(narration.sentence)) }
    val wordBars = remember(placed, reading, narration.word) { wordsIn(narration.word) }
    val press = PdfPress(tap, settings.longPressReads, marked, readingPage, sentenceBars, wordBars) { page, x, y, read ->
        scope.launch {
            val words = textPages.words(page)
            val i = PdfWords.hit(words, x, y)
            if (i == null) {
                host.notify(
                    if (words.isEmpty()) "This page has no text to read aloud. It may be a scanned page."
                    else "Long-press on a word to read aloud from it."
                )
                return@launch
            }
            val word = words[i]
            marked = page to word
            if (read) {
                val at = PdfText.locateWord(textPages.paragraphs(page), words.map { it.text }, i)
                if (at == null) host.notify("That word is in the page header or footer, which isn't read aloud.")
                else host.startNarration(PdfSource(context, file, page, at.first, at.second))
            } else {
                clipboard.setText(AnnotatedString(word.text))
                host.notify("Copied \"${word.text}\"")
            }
            delay(1200)
            if (marked?.second === word) marked = null
        }
    }

    when {
        session.pdfReflow -> PdfReflowView(file, textPages, count, start, settings, narration, host, tap, publish, save)
        settings.pdfPaged -> PdfPagedView(pdf, start, settings, readingPage, host, tap, publish, save, thumbnail, press)
        else -> PdfScrollView(pdf, start, settings, readingPage, host, tap, publish, save, thumbnail, press)
    }
}

/** What a page picture needs to handle taps and long-presses on its words. */
private class PdfPress(
    val tap: TapArea,
    val longPressReads: Boolean,
    /** The word just long-pressed (page, word), shown highlighted for a moment. */
    val marked: Pair<Int, PdfWord>?,
    /** The page being read aloud, with the sentence (one box per line) and the word being spoken. */
    val readingPage: Int?,
    val sentence: List<PdfWord>,
    val word: List<PdfWord>,
    /** Long-press at (x, y) as fractions of the whole page; read aloud from there, or copy the word. */
    val onWord: (page: Int, x: Float, y: Float, read: Boolean) -> Unit,
)

@Composable
private fun PdfScrollView(
    pdf: PdfDocument,
    start: Int,
    settings: ReaderSettings,
    readingPage: Int?,
    host: ReaderHost,
    tap: TapArea,
    publish: (Int) -> Unit,
    save: (Int) -> Unit,
    thumbnail: suspend (Int, Int) -> Bitmap?,
    press: PdfPress,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = start)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    LaunchedEffect(state, settings.pageFlip) {
        host.nav = listNav(state, scope, density, settings.pageFlip == PageFlip.SLIDE, itemsArePages = true, thumbnail = thumbnail)
    }
    TrackPage(state, { state.firstVisibleItemIndex }, publish, save)
    LaunchedEffect(readingPage) { readingPage?.let { state.reveal(it) } }
    val look = PdfLook(settings)

    BoxWithConstraints(Modifier.fillMaxSize().background(look.margin).tapArea(tap)) {
        val widthPx = (with(LocalDensity.current) { maxWidth.roundToPx() } * settings.pdfQuality.scale).toInt()
        LazyColumn(state = state, verticalArrangement = Arrangement.spacedBy(settings.pdfPageGap.dp), modifier = Modifier.fillMaxSize()) {
            itemsIndexed(pdf.aspectRatios) { index, ratio ->
                PdfPageImage(pdf, index, widthPx, settings.pdfCropMargins, ratio, look, press, Modifier.fillMaxWidth())
            }
        }
    }
}

/** One page per screen, turned sideways like a book. */
@Composable
private fun PdfPagedView(
    pdf: PdfDocument,
    start: Int,
    settings: ReaderSettings,
    readingPage: Int?,
    host: ReaderHost,
    tap: TapArea,
    publish: (Int) -> Unit,
    save: (Int) -> Unit,
    thumbnail: suspend (Int, Int) -> Bitmap?,
    press: PdfPress,
) {
    val count = pdf.pageCount
    val pager = rememberPagerState(initialPage = start) { count }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager, settings.pageFlip) {
        val animate = settings.pageFlip == PageFlip.SLIDE
        host.nav = ReaderNav(
            goToPage = { i -> scope.launch { pager.scrollToPage(i) } },
            turn = { d ->
                scope.launch {
                    val target = (pager.currentPage + d).coerceIn(0, count - 1)
                    if (animate) pager.animateScrollToPage(target) else pager.scrollToPage(target)
                }
            },
            autoScroll = { speed ->
                autoTurnPages(host, speed) {
                    if (pager.currentPage < count - 1) { pager.animateScrollToPage(pager.currentPage + 1); true } else false
                }
            },
            thumbnail = thumbnail,
        )
    }
    TrackPage(pager, { pager.currentPage }, publish, save)
    LaunchedEffect(readingPage) { readingPage?.let { if (it != pager.currentPage) pager.animateScrollToPage(it) } }
    val look = PdfLook(settings)

    BoxWithConstraints(Modifier.fillMaxSize().background(look.margin).tapArea(tap)) {
        val maxW = with(LocalDensity.current) { maxWidth.roundToPx() }
        val maxH = with(LocalDensity.current) { maxHeight.roundToPx() }
        HorizontalPager(state = pager, pageSpacing = settings.pdfPageGap.dp, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { index ->
            val ratio = pdf.aspectRatios[index]
            val fitWidth = minOf(maxW.toFloat(), maxH * ratio)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PdfPageImage(pdf, index, (fitWidth * settings.pdfQuality.scale).toInt(), settings.pdfCropMargins, ratio, look, press, Modifier)
            }
        }
    }
}

@Composable
private fun PdfPageImage(
    pdf: PdfDocument,
    index: Int,
    widthPx: Int,
    crop: Boolean,
    pageRatio: Float,
    look: PdfLook,
    press: PdfPress,
    modifier: Modifier,
) {
    val bitmap by produceState<Bitmap?>(null, index, widthPx, crop, look.night) { value = pdf.render(index, widthPx, crop, look.night) }
    val b = bitmap
    val ratio = b?.let { it.width.toFloat() / it.height } ?: pageRatio
    // The part of the page this picture shows, to turn a finger position into a place on the page.
    val shown by rememberUpdatedState(if (b != null) pdf.boxOf(index, crop) else FULL_PAGE)
    val p by rememberUpdatedState(press)
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var menuAt by remember { mutableStateOf<Offset?>(null) }
    val mark = press.marked?.takeIf { it.first == index }?.second
    val markColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
    val reading = index == press.readingPage
    val sentence = if (reading) press.sentence else emptyList()
    val spoken = if (reading) press.word else emptyList()
    val sentenceColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.26f)
    val spokenColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
    val density = LocalDensity.current

    fun send(pos: Offset, read: Boolean) {
        val size = coords?.size ?: return
        if (size.width == 0 || size.height == 0) return
        p.onWord(index, shown.left + pos.x / size.width * shown.width(), shown.top + pos.y / size.height * shown.height(), read)
    }

    Box(
        modifier
            .aspectRatio(ratio)
            .background(look.pageBackground)
            .onGloballyPositioned { coords = it }
            .pointerInput(index) {
                detectTapGestures(
                    onTap = { pos -> coords?.takeIf { it.isAttached }?.let { p.tap.tapInWindow(it.localToWindow(pos)) } },
                    onLongPress = { pos -> if (p.longPressReads) send(pos, true) else menuAt = pos },
                )
            }
            .drawWithContent {
                drawContent()
                if (shown.width() > 0f && shown.height() > 0f) {
                    // A soft rounded bar over a word or line, in the page picture's coordinates.
                    fun bar(w: PdfWord, color: Color) {
                        val pad = 2.5.dp.toPx()
                        val l = (w.left - shown.left) / shown.width() * size.width - pad
                        val t = (w.top - shown.top) / shown.height() * size.height - pad
                        val r = (w.right - shown.left) / shown.width() * size.width + pad
                        val bt = (w.bottom - shown.top) / shown.height() * size.height + pad
                        drawRoundRect(color, Offset(l, t), Size(r - l, bt - t), CornerRadius(4.dp.toPx()))
                    }
                    sentence.forEach { bar(it, sentenceColor) }
                    spoken.forEach { bar(it, spokenColor) }
                    mark?.let { bar(it, markColor) }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (b == null) CircularProgressIndicator(Modifier.size(32.dp))
        else Image(b.asImageBitmap(), "Page ${index + 1}", Modifier.fillMaxSize(), contentScale = ContentScale.Fit, colorFilter = look.filter)
        val at = menuAt
        DropdownMenu(
            expanded = at != null,
            onDismissRequest = { menuAt = null },
            offset = if (at == null) DpOffset.Zero else with(density) { DpOffset(at.x.toDp(), (at.y - (coords?.size?.height ?: 0)).toDp()) },
        ) {
            DropdownMenuItem(text = { Text("Read from here") }, onClick = { at?.let { send(it, true) }; menuAt = null })
            DropdownMenuItem(text = { Text("Copy word") }, onClick = { at?.let { send(it, false) }; menuAt = null })
        }
    }
}

/**
 * The PDF's text laid out to fit the screen ("text reflow"), page by page, in the reading
 * font size and theme. Long-press a word to read aloud from it, as in TXT books.
 */
@Composable
private fun PdfReflowView(
    file: File,
    pages: PdfTextPages,
    count: Int,
    start: Int,
    settings: ReaderSettings,
    narration: NarrationState,
    host: ReaderHost,
    tap: TapArea,
    publish: (Int) -> Unit,
    save: (Int) -> Unit,
) {
    val context = LocalContext.current
    val state = rememberLazyListState(initialFirstVisibleItemIndex = start)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    LaunchedEffect(state, settings.pageFlip) {
        host.nav = listNav(state, scope, density, settings.pageFlip == PageFlip.SLIDE, itemsArePages = true)
    }
    TrackPage(state, { state.firstVisibleItemIndex }, publish, save)
    val reading = narration.block?.position as? BlockPosition.Pdf
    LaunchedEffect(reading?.page) { reading?.page?.let { state.reveal(it) } }

    val colors = settings.theme.colors()
    val accent = MaterialTheme.colorScheme.secondary
    val gap = (10 * settings.lineHeight).dp
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize().background(colors.background).tapArea(tap),
        contentPadding = PaddingValues(horizontal = (20 * settings.pageMargins).dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        items(count) { page ->
            val paragraphs by produceState<List<String>?>(null, page) { value = pages.paragraphs(page) }
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Text(
                    "— ${page + 1} —",
                    color = colors.text.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                val list = paragraphs
                when {
                    list == null -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) }
                    list.isEmpty() -> Text(
                        "No text on this page. It may be a picture or a scanned page; turn off text reflow to see it.",
                        color = colors.text.copy(alpha = 0.6f),
                        fontSize = 15.sp,
                    )
                    else -> list.forEachIndexed { i, paragraph ->
                        val here = reading?.page == page && reading.paragraph == i
                        TxtParagraph(
                            text = paragraph,
                            color = colors.text,
                            fontSize = (18 * settings.fontScale).toFloat(),
                            lineHeight = settings.lineHeight.toFloat(),
                            fontFamily = settings.font.family(),
                            sentence = if (here) narration.sentence else null,
                            word = if (here) narration.word else null,
                            accent = accent,
                            longPressReads = settings.longPressReads,
                            onReadFrom = { offset -> host.startNarration(PdfSource(context, file, page, i, offset)) },
                            onTap = tap::tapInWindow,
                        )
                    }
                }
            }
        }
    }
}

// ---------- TXT ----------

@Composable
internal fun TxtReader(session: ReaderSession, settings: ReaderSettings, narration: NarrationState, host: ReaderHost) {
    val book by produceState<Result<PlainTextBook>?>(null, session.book.filePath) {
        value = withContext(Dispatchers.IO) { runCatching { PlainText.parse(File(session.book.filePath).readBytes()) } }
    }
    val colors = settings.theme.colors()
    val result = book ?: return Centered { CircularProgressIndicator() }
    val text = result.getOrElse {
        return Centered { Text("This text file could not be read.", color = colors.text, modifier = Modifier.padding(24.dp)) }
    }
    if (text.paragraphs.isEmpty()) return Centered { Text("This text file has no readable text.", color = colors.text, modifier = Modifier.padding(24.dp)) }

    val count = text.paragraphs.size
    val chapters = remember(text) { text.chapters.map { OutlineEntry(it.title, 0, it.paragraphIndex) } }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = indexFrom(session.lastLocation, "para").coerceAtMost(count - 1))
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    val readFrom: (Int, Int) -> Unit = { para, offset -> host.startNarration(TxtSource(text.paragraphs, para, offset)) }
    val tap = rememberTapArea(host, settings.tapEdgesTurn)

    LaunchedEffect(text) {
        host.toc = text.chapters.map { c -> TocEntry(c.title, 0) { host.nav?.goToPage(c.paragraphIndex) } }
        host.hooks = NarrationHooks(
            playFromScreen = { readFrom(state.firstVisibleItemIndex, 0) },
            resumeListening = resume@{ saved ->
                val (para, offset) = parseTxtListen(saved) ?: return@resume false
                if (para >= count) false else { readFrom(para, offset); true }
            },
        )
    }
    LaunchedEffect(state, settings.pageFlip) {
        host.nav = listNav(state, scope, density, settings.pageFlip == PageFlip.SLIDE, itemsArePages = false)
    }
    val progressOf: (Int) -> Double = { i -> if (count <= 1) 1.0 else i.toDouble() / (count - 1) }
    TrackPage(
        state,
        { state.firstVisibleItemIndex },
        publish = { i -> host.position = ReaderPosition(i, count, chapterAt(chapters, i), "${(progressOf(i) * 100).toInt()}%") },
        save = { i -> host.save("para:$i", progressOf(i)) },
    )

    val readingPara = (narration.block?.position as? BlockPosition.Txt)?.paragraph
    LaunchedEffect(readingPara) { readingPara?.let { state.reveal(it) } }

    val accent = MaterialTheme.colorScheme.secondary
    val fontSize = (18 * settings.fontScale).sp
    val side = (20 * settings.pageMargins).dp
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize().tapArea(tap),
        contentPadding = PaddingValues(horizontal = side, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy((10 * settings.lineHeight).dp),
    ) {
        itemsIndexed(text.paragraphs) { index, paragraph ->
            TxtParagraph(
                text = paragraph,
                color = colors.text,
                fontSize = fontSize.value,
                lineHeight = settings.lineHeight.toFloat(),
                fontFamily = settings.font.family(),
                sentence = if (index == readingPara) narration.sentence else null,
                word = if (index == readingPara) narration.word else null,
                accent = accent,
                longPressReads = settings.longPressReads,
                onReadFrom = { offset -> readFrom(index, offset) },
                onTap = tap::tapInWindow,
            )
        }
    }
}

/**
 * One paragraph. Long-press a word to read from it (or, with the menu setting, choose
 * "Read from here" or "Copy"). A short tap goes to the page's tap areas (menu, turn page).
 * TalkBack users get "read aloud" as a custom action.
 */
@Composable
private fun TxtParagraph(
    text: String,
    color: Color,
    fontSize: Float,
    lineHeight: Float,
    fontFamily: FontFamily?,
    sentence: Segment?,
    word: Segment?,
    accent: Color,
    longPressReads: Boolean,
    onReadFrom: (Int) -> Unit,
    onTap: (Offset) -> Unit,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var menuAt by remember { mutableStateOf<Int?>(null) }
    val direct by rememberUpdatedState(longPressReads)
    val read by rememberUpdatedState(onReadFrom)
    val tap by rememberUpdatedState(onTap)
    val clipboard = LocalClipboardManager.current

    // The spoken word sits on a solid rounded pill, so its text turns dark to stay readable.
    val annotated = remember(text, word) {
        buildAnnotatedString {
            append(text)
            word?.let { seg ->
                val s = seg.start.coerceIn(0, text.length)
                val e = seg.end.coerceIn(s, text.length)
                if (s < e) addStyle(SpanStyle(color = Color(0xFF1D2433)), s, e)
            }
        }
    }
    val sentenceColor = accent.copy(alpha = 0.22f)

    Box {
        Text(
            annotated,
            color = color,
            fontSize = fontSize.sp,
            lineHeight = lineHeight.em,
            fontFamily = fontFamily,
            onTextLayout = { layout = it },
            modifier = Modifier
                .drawBehind {
                    val l = layout ?: return@drawBehind
                    val length = l.layoutInput.text.length
                    // Rounded bars behind a range of the text, one per line it covers.
                    fun bars(seg: Segment, barColor: Color) {
                        val s = seg.start.coerceIn(0, length)
                        val e = seg.end.coerceIn(s, length)
                        if (s >= e) return
                        val first = l.getLineForOffset(s)
                        val last = l.getLineForOffset(e - 1)
                        val padX = 3.dp.toPx()
                        for (line in first..last) {
                            val left = if (line == first) l.getHorizontalPosition(s, true) else l.getLineLeft(line)
                            val right = if (line == last && l.getLineForOffset(e) == last) l.getHorizontalPosition(e, true) else l.getLineRight(line)
                            val top = l.getLineTop(line)
                            val bottom = l.getLineBottom(line)
                            val inset = ((bottom - top) - fontSize.sp.toPx() * 1.4f).coerceAtLeast(0f) / 2
                            drawRoundRect(
                                barColor,
                                Offset(minOf(left, right) - padX, top + inset),
                                Size(kotlin.math.abs(right - left) + 2 * padX, bottom - top - 2 * inset),
                                CornerRadius(6.dp.toPx()),
                            )
                        }
                    }
                    sentence?.let { bars(it, sentenceColor) }
                    word?.let { bars(it, accent) }
                }
                .onGloballyPositioned { coords = it }
                .pointerInput(text) {
                    detectTapGestures(
                        onTap = { pos -> coords?.takeIf { it.isAttached }?.let { tap(it.localToWindow(pos)) } },
                        onLongPress = { pos ->
                            val offset = layout?.getOffsetForPosition(pos) ?: return@detectTapGestures
                            if (direct) read(offset) else menuAt = offset
                        },
                    )
                }
                .semantics { onLongClick(label = "Read aloud from this paragraph") { read(0); true } },
        )
        DropdownMenu(expanded = menuAt != null, onDismissRequest = { menuAt = null }) {
            DropdownMenuItem(text = { Text("Read from here") }, onClick = { menuAt?.let(read); menuAt = null })
            DropdownMenuItem(text = { Text("Copy paragraph") }, onClick = { clipboard.setText(AnnotatedString(text)); menuAt = null })
        }
    }
}

/** The typeface for text the app lays out itself (TXT books and PDF text reflow). */
internal fun ReadingFont.family(): FontFamily? = when (this) {
    ReadingFont.ORIGINAL -> null
    ReadingFont.SERIF -> FontFamily.Serif
    ReadingFont.SANS -> FontFamily.SansSerif
    ReadingFont.MONO -> FontFamily.Monospace
}
