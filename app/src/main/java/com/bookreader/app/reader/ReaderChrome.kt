package com.bookreader.app.reader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.BatteryManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bookreader.app.narration.NarrationSource
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------- Look ----------

/** One set of reader chrome colours. */
@Immutable
internal class ChromeColors(
    val dark: Boolean,
    /** Top bar (also behind the status bar) and what's drawn on it. */
    val topBar: Color,
    val onTopBar: Color,
    /** Bottom panel (slider row, toolbar, floating bars) and what's drawn on it. */
    val bottomBar: Color,
    val onBottomBar: Color,
    val onBottomBarDim: Color,
    /** Line between the slider row and the toolbar. */
    val barDivider: Color,
    /** Menus, sheets, dialogs and the options page. */
    val panel: Color,
    val card: Color,
    val button: Color,
    val divider: Color,
    val text: Color,
    val textDim: Color,
    val accent: Color,
    val sliderActive: Color,
    val sliderInactive: Color,
    val fab: Color,
    val fabIcon: Color,
    val switchOff: Color,
)

/** Night: grey bars and menus, sampled from Moon+ Reader's night screens. */
internal val NightChrome = ChromeColors(
    dark = true,
    topBar = Color(0xFF424242),
    onTopBar = Color(0xFFEEEEEE),
    bottomBar = Color(0xFF424242),
    onBottomBar = Color(0xFFEEEEEE),
    onBottomBarDim = Color(0xFF9E9E9E),
    barDivider = Color(0xFF505050),
    panel = Color(0xFF505050),
    card = Color(0xFF474747),
    button = Color(0xFF6C6C6C),
    divider = Color(0xFF7A7A7A),
    text = Color(0xFFEEEEEE),
    textDim = Color(0xFFB4B4B4),
    accent = Color(0xFF2B99F2),
    sliderActive = Color(0xFF1C96C7),
    sliderInactive = Color(0xFFAEAEAE),
    fab = Color(0xFFB4B4B4),
    fabIcon = Color(0xFF6A6B6D),
    switchOff = Color(0xFF4B4A48),
)

/** Day: blue top bar with white text, light grey bottom panel, sampled from Moon+ Reader's day screens. */
internal val DayChrome = ChromeColors(
    dark = false,
    topBar = Color(0xFF1D89E4),
    onTopBar = Color.White,
    bottomBar = Color(0xFFE0E0E0),
    onBottomBar = Color(0xFF707070),
    onBottomBarDim = Color(0xFF9E9E9E),
    barDivider = Color(0xFFCFCFCF),
    panel = Color(0xFFF5F5F5),
    card = Color.White,
    button = Color(0xFFE0E0E0),
    divider = Color(0xFFBDBDBD),
    text = Color(0xFF212121),
    textDim = Color(0xFF707070),
    accent = Color(0xFF1D89E4),
    sliderActive = Color(0xFF1A96CA),
    sliderInactive = Color(0xFFB4B4B4),
    fab = Color(0xFFC3C3C3),
    fabIcon = Color(0xFF7B7C80),
    switchOff = Color(0xFFE0E0E0),
)

internal val LocalChrome = staticCompositionLocalOf { NightChrome }

/** The current chrome colours, readable from any composable as `Chrome.text` etc. */
internal object Chrome {
    val topBar: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.topBar
    val onTopBar: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.onTopBar
    val bottomBar: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.bottomBar
    val onBottomBar: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.onBottomBar
    val onBottomBarDim: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.onBottomBarDim
    val barDivider: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.barDivider
    val panel: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.panel
    val card: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.card
    val button: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.button
    val divider: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.divider
    val text: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.text
    val textDim: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.textDim
    val accent: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.accent
    val sliderActive: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.sliderActive
    val sliderInactive: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.sliderInactive
    val fab: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.fab
    val fabIcon: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.fabIcon
    val switchOff: Color @Composable @ReadOnlyComposable get() = LocalChrome.current.switchOff
}

/**
 * Material theme for everything drawn over the page. Moon+ Reader's chrome follows the
 * reading theme: blue and light grey by day, dark grey by night.
 */
@Composable
internal fun ChromeTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalChrome provides if (dark) NightChrome else DayChrome) {
        ChromeMaterialTheme(dark, content)
    }
}

@Composable
private fun ChromeMaterialTheme(dark: Boolean, content: @Composable () -> Unit) {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = base.copy(
            primary = Chrome.accent,
            onPrimary = Color.White,
            secondary = Color(0xFFF2B84B), // reading-aloud highlight, the app's amber
            onSecondary = Color(0xFF1D2433),
            background = Chrome.panel,
            surface = Chrome.panel,
            onSurface = Chrome.text,
            onBackground = Chrome.text,
            surfaceVariant = Chrome.card,
            onSurfaceVariant = Chrome.textDim,
            surfaceContainerLowest = Chrome.panel,
            surfaceContainerLow = Chrome.panel,
            surfaceContainer = Chrome.panel,
            surfaceContainerHigh = Chrome.panel,
            surfaceContainerHighest = Chrome.card,
            outline = Chrome.divider,
            outlineVariant = Chrome.divider,
            secondaryContainer = Chrome.button,
            onSecondaryContainer = Chrome.text,
        ),
        content = content,
    )
}

@Composable
internal fun chromeSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Chrome.accent,
    checkedBorderColor = Chrome.accent,
    uncheckedThumbColor = Chrome.textDim,
    uncheckedTrackColor = Chrome.switchOff,
    uncheckedBorderColor = Chrome.textDim,
)

// ---------- What each format shares with the chrome ----------

/** Where the reader is, for the status line and the page slider. */
data class ReaderPosition(
    val page: Int = 0,
    val pageCount: Int = 0,
    val chapter: String? = null,
    /** Shown on the right of the status line and next to the slider, e.g. "29/535" or "45%". */
    val label: String = "",
)

/** How the chrome moves through the book. Each format provides one. */
class ReaderNav(
    val goToPage: (Int) -> Unit,
    /** +1 next screen or page, -1 previous. */
    val turn: (Int) -> Unit,
    /** Scrolls or turns pages on its own until cancelled or the end; reads the speed (1..10) as it goes. */
    val autoScroll: suspend (speed: () -> Int) -> Unit,
    /** PDF only: a small render of a page for the thumbnail strip. */
    val thumbnail: (suspend (page: Int, widthPx: Int) -> Bitmap?)? = null,
)

/** Shared between the reader screen and the format views (EPUB, PDF, TXT). */
@Stable
class ReaderHost(
    val save: (String, Double) -> Unit,
    val startNarration: (NarrationSource) -> Unit,
) {
    var onTap: (TapZone) -> Unit = {}
    var toc by mutableStateOf<List<TocEntry>>(emptyList())
    var hooks by mutableStateOf<NarrationHooks?>(null)
    var nav by mutableStateOf<ReaderNav?>(null)
    var position by mutableStateOf(ReaderPosition())
}

/** Turns taps anywhere in a reading area, including on text, into [TapZone]s. */
@Stable
internal class TapArea(private val host: ReaderHost) {
    var coords: LayoutCoordinates? = null
    var edgesTurnPages = true

    fun tapInWindow(p: Offset) {
        val c = coords?.takeIf { it.isAttached } ?: return
        val local = c.windowToLocal(p)
        host.onTap(tapZone(local.x, local.y, c.size.width.toFloat(), c.size.height.toFloat(), edgesTurnPages))
    }
}

@Composable
internal fun rememberTapArea(host: ReaderHost, edgesTurnPages: Boolean): TapArea =
    remember(host) { TapArea(host) }.also { it.edgesTurnPages = edgesTurnPages }

internal fun Modifier.tapArea(area: TapArea): Modifier =
    onGloballyPositioned { area.coords = it }
        .pointerInput(area) {
            detectTapGestures { pos -> area.coords?.takeIf { it.isAttached }?.let { area.tapInWindow(it.localToWindow(pos)) } }
        }

// ---------- Top bar ----------

@Composable
internal fun ReaderTopBar(
    title: String,
    showReflow: Boolean,
    reflowOn: Boolean,
    onBack: () -> Unit,
    onReflow: () -> Unit,
    menu: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().shadow(6.dp).background(Chrome.topBar).statusBarsPadding().height(64.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBackIos, "Back to library", tint = Chrome.onTopBar) }
        Text(
            title,
            color = Chrome.onTopBar,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (showReflow) {
            IconButton(onClick = onReflow) { ReflowIcon(on = reflowOn) }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "More options", tint = Chrome.onTopBar) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) { menu { menuOpen = false } }
        }
    }
}

/** A page with an "A" on it: switches a PDF between page view and reflowed text. */
@Composable
private fun ReflowIcon(on: Boolean) {
    Box(
        Modifier
            .size(width = 24.dp, height = 28.dp)
            .background(if (on) Chrome.accent else Color.White, RoundedCornerShape(topStart = 2.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("A", color = if (on) Color.White else Chrome.topBar, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

// ---------- Bottom panel ----------

@Composable
internal fun PageSliderRow(position: ReaderPosition, nav: ReaderNav?) {
    val count = position.pageCount
    var dragging by remember { mutableStateOf<Float?>(null) }
    val fraction = dragging ?: if (count > 1) position.page.toFloat() / (count - 1) else 0f
    val target = if (count > 1) (fraction * (count - 1)).toInt() else 0
    val label = if (dragging != null && position.label.contains('/')) "${target + 1}/$count" else position.label
    Row(
        Modifier.fillMaxWidth().shadow(6.dp).background(Chrome.bottomBar).height(60.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { nav?.goToPage((position.page - 1).coerceAtLeast(0)) }, enabled = count > 0) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous page", tint = Chrome.onBottomBarDim)
        }
        ThinSlider(
            value = fraction,
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { f -> nav?.goToPage(if (count > 1) (f * (count - 1)).toInt() else 0) }
                dragging = null
            },
            enabled = count > 1,
            modifier = Modifier.weight(1f),
        )
        Text(label, color = Chrome.onBottomBar, fontSize = 20.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
        IconButton(onClick = { nav?.goToPage((position.page + 1).coerceAtMost(count - 1)) }, enabled = count > 0) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next page", tint = Chrome.onBottomBarDim)
        }
    }
}

/** A classic slider: thin blue/grey line and a round white thumb (Material's newer thick style looks different). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    steps: Int = 0,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        modifier = modifier,
        thumb = {
            Box(Modifier.size(26.dp).shadow(3.dp, CircleShape).background(Color.White, CircleShape))
        },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val f = if (span > 0f) ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f) else 0f
            val inactive = Chrome.sliderInactive
            val active = Chrome.sliderActive
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val y = size.height / 2
                drawLine(inactive, Offset(0f, y), Offset(size.width, y), size.height, StrokeCap.Round)
                drawLine(active, Offset(0f, y), Offset(size.width * f, y), size.height, StrokeCap.Round)
            }
        },
    )
}

/** The six tools under the slider, as in Moon+ Reader. */
@Composable
internal fun ReaderToolbar(
    speaking: Boolean,
    onRotate: () -> Unit,
    dayNightMenu: @Composable (open: Boolean, dismiss: () -> Unit) -> Unit,
    onSpeak: () -> Unit,
    onAutoScroll: () -> Unit,
    onContents: () -> Unit,
    onMore: () -> Unit,
) {
    var dayNight by remember { mutableStateOf(false) }
    HorizontalDivider(color = Chrome.barDivider, modifier = Modifier.background(Chrome.bottomBar))
    Row(
        Modifier.fillMaxWidth().background(Chrome.bottomBar).navigationBarsPadding().height(72.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolButton(Icons.Filled.ScreenRotation, "Screen rotation", onClick = onRotate)
        Box {
            ToolButton(Icons.Filled.Contrast, "Day or night mode") { dayNight = true }
            dayNightMenu(dayNight) { dayNight = false }
        }
        ToolButton(Icons.AutoMirrored.Filled.VolumeUp, if (speaking) "Pause reading aloud" else "Read aloud", tint = if (speaking) Chrome.accent else Chrome.onBottomBar, onClick = onSpeak)
        ToolButton(Icons.Filled.UnfoldMore, "Auto scroll", onClick = onAutoScroll)
        ToolButton(Icons.AutoMirrored.Filled.FormatListBulleted, "Contents", onClick = onContents)
        ToolButton(Icons.Filled.MoreHoriz, "Reading settings", onClick = onMore)
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, tint: Color = Chrome.onBottomBar, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(30.dp))
    }
}

/** Round grey buttons above the panel (PDF): page-by-page vs. continuous scroll, and trimming white borders. */
@Composable
internal fun PdfFloatingButtons(paged: Boolean, cropped: Boolean, onTogglePaged: () -> Unit, onToggleCrop: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(end = 20.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.End),
    ) {
        RoundButton(if (paged) Icons.Filled.ViewCarousel else Icons.Filled.ViewAgenda, if (paged) "Switch to continuous scroll" else "Switch to page by page", onTogglePaged)
        RoundButton(Icons.Filled.Crop, if (cropped) "Show white margins" else "Trim white margins", onToggleCrop, active = cropped)
    }
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, onClick: () -> Unit, active: Boolean = false) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Chrome.fab.copy(alpha = 0.92f))
            .border(2.dp, if (active) Chrome.accent else Color.White.copy(alpha = 0.6f), CircleShape)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = Chrome.fabIcon, modifier = Modifier.size(26.dp))
    }
}

/** Small page pictures above the slider (PDF, when turned on in PDF options). */
@Composable
internal fun ThumbnailStrip(position: ReaderPosition, nav: ReaderNav) {
    val thumb = nav.thumbnail ?: return
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (position.page - 2).coerceAtLeast(0))
    LaunchedEffect(position.page) { state.animateScrollToItem((position.page - 2).coerceAtLeast(0)) }
    val widthPx = with(LocalDensity.current) { 64.dp.roundToPx() }
    LazyRow(
        state = state,
        modifier = Modifier.fillMaxWidth().background(Chrome.bottomBar).height(132.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(position.pageCount) { page ->
            val bitmap by produceState<Bitmap?>(null, page) { value = thumb(page, widthPx) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { nav.goToPage(page) }) {
                Box(
                    Modifier
                        .width(64.dp)
                        .aspectRatio(0.72f)
                        .background(Color.White)
                        .border(if (page == position.page) 3.dp else 0.dp, Chrome.accent),
                ) {
                    bitmap?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Fit) }
                }
                Text("${page + 1}", color = Chrome.onBottomBarDim, fontSize = 11.sp)
            }
        }
    }
}

// ---------- Reading aloud and auto scroll bars ----------

@Composable
internal fun TtsBar(
    playing: Boolean,
    timerOn: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    onTimer: () -> Unit,
    onVoices: () -> Unit,
) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Chrome.bottomBar.copy(alpha = 0.96f))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) { Icon(Icons.Filled.SkipPrevious, "Previous sentence", tint = Chrome.onBottomBar) }
        IconButton(onClick = onPlayPause) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) "Pause" else "Resume", tint = Chrome.onBottomBar)
        }
        IconButton(onClick = onNext) { Icon(Icons.Filled.SkipNext, "Next sentence", tint = Chrome.onBottomBar) }
        IconButton(onClick = onStop) { Icon(Icons.Filled.Stop, "Stop reading aloud", tint = Chrome.onBottomBar) }
        IconButton(onClick = onTimer) {
            Icon(Icons.Filled.Bedtime, if (timerOn) "Sleep timer on" else "Sleep timer", tint = if (timerOn) Chrome.accent else Chrome.onBottomBar)
        }
        IconButton(onClick = onVoices) { Icon(Icons.Filled.RecordVoiceOver, "Voice and speed", tint = Chrome.onBottomBar) }
    }
}

@Composable
internal fun AutoScrollBar(speed: Int, onSpeed: (Int) -> Unit, onStop: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Chrome.bottomBar.copy(alpha = 0.96f))
            .padding(start = 18.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Auto scroll", color = Chrome.onBottomBar, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = { onSpeed(speed - 1) }) { Icon(Icons.Filled.Remove, "Slower", tint = Chrome.accent) }
        Text("$speed", color = Chrome.onBottomBar, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
        IconButton(onClick = { onSpeed(speed + 1) }) { Icon(Icons.Filled.Add, "Faster", tint = Chrome.accent) }
        IconButton(onClick = onStop) { Icon(Icons.Filled.Close, "Stop auto scroll", tint = Chrome.onBottomBar) }
    }
}

// ---------- Day / night menu ----------

/** "Day/Night Mode" pop-up: title, divider, then the choices, in large text as in Moon+ Reader. */
@Composable
internal fun DayNightMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    title: String,
    items: List<Pair<String, () -> Unit>>,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = Chrome.panel,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            title,
            color = Chrome.text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 24.dp, end = 32.dp, top = 12.dp, bottom = 14.dp),
        )
        HorizontalDivider(color = Chrome.divider, modifier = Modifier.padding(horizontal = 24.dp))
        items.forEach { (label, action) ->
            DropdownMenuItem(
                text = { Text(label, color = Chrome.text, fontSize = 21.sp) },
                onClick = { onDismiss(); action() },
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier.height(64.dp),
            )
        }
    }
}

// ---------- Status line ----------

/** The line under the page: battery and time on the left, chapter in the middle, page on the right. */
@Composable
internal fun StatusLine(position: ReaderPosition, color: Color) {
    val battery = rememberBatteryLevel()
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(15_000)
        }
    }
    val time = remember(now) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(now) }
    Row(
        Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(listOfNotNull(battery?.toString(), time).joinToString(" "), color = color, fontSize = 14.sp)
        BoxWithConstraints(Modifier.weight(1f).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            // Long chapter names keep their start and end, as in Moon+ Reader: "From Language…nguage Models".
            val maxChars = (maxWidth.value / 7.6f).toInt()
            Text(middleEllipsis(position.chapter.orEmpty(), maxChars), color = color, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Clip)
        }
        Text(position.label, color = color, fontSize = 14.sp)
    }
}

@Composable
private fun rememberBatteryLevel(): Int? {
    val context = LocalContext.current
    var level by remember {
        mutableIntStateOf(context.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1)
    }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val l = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (l >= 0 && scale > 0) level = l * 100 / scale
            }
        }
        // The battery broadcast is sticky, so registering also delivers the current level.
        ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return level.takeIf { it >= 0 }
}
