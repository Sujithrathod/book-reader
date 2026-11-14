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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.SwipeVertical
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewCarousel
import com.bookreader.app.data.ReadingTheme
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

/** Night: near-black surfaces with a soft blue accent. */
internal val NightChrome = ChromeColors(
    dark = true,
    topBar = Color(0xFF1F2023),
    onTopBar = Color(0xFFF1F1F3),
    bottomBar = Color(0xFF1F2023),
    onBottomBar = Color(0xFFF1F1F3),
    onBottomBarDim = Color(0xFF9A9CA3),
    barDivider = Color(0xFF2E3035),
    panel = Color(0xFF151618),
    card = Color(0xFF212327),
    button = Color(0xFF30333A),
    divider = Color(0xFF34373D),
    text = Color(0xFFF1F1F3),
    textDim = Color(0xFFA0A3AA),
    accent = Color(0xFF7FB2FF),
    sliderActive = Color(0xFF7FB2FF),
    sliderInactive = Color(0xFF3F434A),
    fab = Color(0xFF2A2C31),
    fabIcon = Color(0xFFE4E5E8),
    switchOff = Color(0xFF2E3035),
)

/** Day (light and sepia pages): white surfaces with a blue accent. */
internal val DayChrome = ChromeColors(
    dark = false,
    topBar = Color.White,
    onTopBar = Color(0xFF1D1F23),
    bottomBar = Color.White,
    onBottomBar = Color(0xFF1D1F23),
    onBottomBarDim = Color(0xFF7A7E87),
    barDivider = Color(0xFFECEDEF),
    panel = Color(0xFFF3F4F6),
    card = Color.White,
    button = Color(0xFFE9EBEF),
    divider = Color(0xFFE4E6EA),
    text = Color(0xFF1D1F23),
    textDim = Color(0xFF6B6F78),
    accent = Color(0xFF2F6FED),
    sliderActive = Color(0xFF2F6FED),
    sliderInactive = Color(0xFFD9DCE2),
    fab = Color.White,
    fabIcon = Color(0xFF3C4048),
    switchOff = Color(0xFFE4E6EA),
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
    /** Text and icons drawn on top of [accent]. */
    val onAccent: Color @Composable @ReadOnlyComposable get() = if (LocalChrome.current.dark) Color(0xFF0C1D36) else Color.White
}

/** Material theme for everything drawn over the page; it follows the reading theme (day or night). */
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
            onPrimary = Chrome.onAccent,
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
    /** Shows a short message at the bottom of the screen. */
    var notify: (String) -> Unit = {}
    var toc by mutableStateOf<List<TocEntry>>(emptyList())
    var hooks by mutableStateOf<NarrationHooks?>(null)
    var nav by mutableStateOf<ReaderNav?>(null)
    var position by mutableStateOf(ReaderPosition())
    /** Auto scroll in a paged view: how far the wait for the next page turn has got, 0..1. */
    var autoTurn by mutableStateOf<Float?>(null)
}

/** Auto scroll for paged views: waits, showing the wait in [ReaderHost.autoTurn], then turns the page. */
internal suspend fun autoTurnPages(host: ReaderHost, speed: () -> Int, next: suspend () -> Boolean) {
    try {
        while (true) {
            var elapsed = 0f
            while (true) {
                val total = autoTurnSeconds(speed())
                if (elapsed >= total) break
                host.autoTurn = elapsed / total
                delay(100)
                elapsed += 0.1f
            }
            if (!next()) break
        }
    } finally {
        host.autoTurn = null
    }
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
    bookmarked: Boolean,
    onBookmark: () -> Unit,
    onBack: () -> Unit,
    onReflow: () -> Unit,
    menu: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp)
            .background(Chrome.topBar)
            .pointerInput(Unit) { detectTapGestures { } } // taps on the bar don't reach the page
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to library", tint = Chrome.onTopBar) }
        Text(
            title,
            color = Chrome.onTopBar,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        if (showReflow) {
            IconButton(onClick = onReflow) { ReflowIcon(on = reflowOn) }
        }
        IconButton(onClick = onBookmark) {
            Icon(
                if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                if (bookmarked) "Remove bookmark from this page" else "Bookmark this page",
                tint = if (bookmarked) Chrome.accent else Chrome.onTopBar,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, "More options", tint = Chrome.onTopBar) }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = Chrome.card, shape = RoundedCornerShape(16.dp)) { menu { menuOpen = false } }
        }
    }
}

/** A page with an "A" on it: switches a PDF between page view and reflowed text. */
@Composable
private fun ReflowIcon(on: Boolean) {
    Box(
        Modifier
            .size(width = 24.dp, height = 28.dp)
            .background(if (on) Chrome.accent else Chrome.button, RoundedCornerShape(topStart = 4.dp, topEnd = 9.dp, bottomStart = 4.dp, bottomEnd = 4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("A", color = if (on) Chrome.onAccent else Chrome.onTopBar, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

// ---------- Bottom panel ----------

private val PanelShape = RoundedCornerShape(28.dp)

/**
 * The floating card at the bottom of the menu: where you are in the book, a slider to move
 * through it, and five labelled tools with reading aloud in the middle.
 */
@Composable
internal fun ReaderBottomPanel(
    position: ReaderPosition,
    nav: ReaderNav?,
    speaking: Boolean,
    theme: ReadingTheme,
    onTheme: (ReadingTheme) -> Unit,
    onContents: () -> Unit,
    onSpeak: () -> Unit,
    onAutoScroll: () -> Unit,
    onSettings: () -> Unit,
    top: @Composable () -> Unit = {},
) {
    var themes by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            .shadow(18.dp, PanelShape)
            .clip(PanelShape)
            .background(Chrome.bottomBar)
            .pointerInput(Unit) { detectTapGestures { } }, // taps on the card don't reach the page
    ) {
        top()
        PageSlider(position, nav)
        AnimatedVisibility(themes) {
            ThemeRow(theme, onTheme, Modifier.padding(top = 4.dp, bottom = 10.dp))
        }
        HorizontalDivider(color = Chrome.barDivider, modifier = Modifier.padding(horizontal = 20.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolItem(Icons.AutoMirrored.Rounded.FormatListBulleted, "Contents", onClick = onContents)
            ToolItem(if (theme.dark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, "Theme", active = themes) { themes = !themes }
            ListenButton(speaking, onSpeak)
            ToolItem(Icons.Rounded.SwipeVertical, "Auto scroll", onClick = onAutoScroll)
            ToolItem(Icons.Rounded.Settings, "Settings", onClick = onSettings)
        }
    }
}

/** Chapter and page on one line, then the slider with small previous/next arrows. */
@Composable
private fun PageSlider(position: ReaderPosition, nav: ReaderNav?) {
    val count = position.pageCount
    var dragging by remember { mutableStateOf<Float?>(null) }
    val fraction = dragging ?: if (count > 1) position.page.toFloat() / (count - 1) else 0f
    val target = if (count > 1) (fraction * (count - 1)).toInt() else 0
    val label = if (dragging != null && position.label.contains('/')) "${target + 1}/$count" else position.label
    Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            position.chapter?.takeIf { it.isNotBlank() } ?: "Reading progress",
            color = Chrome.onBottomBarDim,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Text(label, color = Chrome.onBottomBar, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { nav?.goToPage((position.page - 1).coerceAtLeast(0)) }, enabled = count > 0) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous page", tint = Chrome.onBottomBarDim)
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
        IconButton(onClick = { nav?.goToPage((position.page + 1).coerceAtMost(count - 1)) }, enabled = count > 0) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next page", tint = Chrome.onBottomBarDim)
        }
    }
}

/** A slim slider: thin line and a small round thumb in the accent colour. */
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
    val thumbColor = if (enabled) Chrome.sliderActive else Chrome.sliderInactive
    val ring = Chrome.bottomBar
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        modifier = modifier,
        thumb = {
            Box(Modifier.size(22.dp).shadow(3.dp, CircleShape).background(ring, CircleShape).padding(3.dp).background(thumbColor, CircleShape))
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

/** An icon with its name under it. */
@Composable
private fun ToolItem(icon: ImageVector, label: String, active: Boolean = false, onClick: () -> Unit) {
    val tint = if (active) Chrome.accent else Chrome.onBottomBar
    Column(
        Modifier.width(64.dp).clip(RoundedCornerShape(16.dp)).clickable(onClickLabel = label, onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        Text(label, color = if (active) Chrome.accent else Chrome.onBottomBarDim, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

/** The main action: a filled round button that starts or pauses reading aloud. */
@Composable
private fun ListenButton(speaking: Boolean, onClick: () -> Unit) {
    val label = if (speaking) "Pause reading aloud" else "Read aloud"
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(
            Modifier.size(52.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(Chrome.accent).clickable(onClickLabel = label, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (speaking) Icons.Rounded.Pause else Icons.Rounded.Headphones, label, tint = Chrome.onAccent, modifier = Modifier.size(26.dp))
        }
        Text(if (speaking) "Pause" else "Listen", color = Chrome.onBottomBar, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Every reading theme as a row of round samples; scrolls sideways when they don't all fit. */
@Composable
internal fun ThemeRow(theme: ReadingTheme, onTheme: (ReadingTheme) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ReadingTheme.entries.forEach { t -> ThemeSwatch(t, selected = t == theme) { onTheme(t) } }
    }
}

/** A round sample of a reading theme: its page colour with "Aa" in its text colour. */
@Composable
internal fun ThemeSwatch(theme: ReadingTheme, selected: Boolean, onClick: () -> Unit) {
    val c = theme.colors()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(66.dp).clip(RoundedCornerShape(14.dp)).clickable(onClickLabel = "${theme.label} theme", onClick = onClick).padding(vertical = 4.dp)) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(c.background)
                .border(if (selected) 2.5.dp else 1.dp, if (selected) Chrome.accent else Chrome.divider, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("Aa", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Text(theme.label, color = if (selected) Chrome.accent else Chrome.onBottomBarDim, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Round buttons above the panel (PDF): page-by-page vs. continuous scroll, and trimming white borders. */
@Composable
internal fun PdfFloatingButtons(paged: Boolean, cropped: Boolean, onTogglePaged: () -> Unit, onToggleCrop: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
    ) {
        RoundButton(if (paged) Icons.Rounded.ViewCarousel else Icons.Rounded.ViewAgenda, if (paged) "Switch to continuous scroll" else "Switch to page by page", onTogglePaged)
        RoundButton(Icons.Rounded.Crop, if (cropped) "Show white margins" else "Trim white margins", onToggleCrop, active = cropped)
    }
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, onClick: () -> Unit, active: Boolean = false) {
    Box(
        Modifier
            .size(46.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(if (active) Chrome.accent else Chrome.fab)
            .clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = if (active) Chrome.onAccent else Chrome.fabIcon, modifier = Modifier.size(24.dp))
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
        modifier = Modifier.fillMaxWidth().height(128.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(position.pageCount) { page ->
            val bitmap by produceState<Bitmap?>(null, page) { value = thumb(page, widthPx) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { nav.goToPage(page) }) {
                Box(
                    Modifier
                        .width(64.dp)
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .border(if (page == position.page) 2.5.dp else 1.dp, if (page == position.page) Chrome.accent else Chrome.divider, RoundedCornerShape(6.dp)),
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
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(Chrome.bottomBar)
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) { Icon(Icons.Rounded.SkipPrevious, "Previous sentence", tint = Chrome.onBottomBar) }
        Box(
            Modifier.padding(horizontal = 4.dp, vertical = 6.dp).size(44.dp).clip(CircleShape).background(Chrome.accent)
                .clickable(onClickLabel = if (playing) "Pause" else "Resume", onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pause" else "Resume", tint = Chrome.onAccent)
        }
        IconButton(onClick = onNext) { Icon(Icons.Rounded.SkipNext, "Next sentence", tint = Chrome.onBottomBar) }
        IconButton(onClick = onStop) { Icon(Icons.Rounded.Stop, "Stop reading aloud", tint = Chrome.onBottomBar) }
        IconButton(onClick = onTimer) {
            Icon(Icons.Rounded.Bedtime, if (timerOn) "Sleep timer on" else "Sleep timer", tint = if (timerOn) Chrome.accent else Chrome.onBottomBar)
        }
        IconButton(onClick = onVoices) { Icon(Icons.Rounded.RecordVoiceOver, "Voice and speed", tint = Chrome.onBottomBar) }
    }
}

/**
 * Auto scroll controls: pause or resume, the speed with slower/faster buttons, and close.
 * In paged views a thin line fills up as the next page turn comes closer.
 */
@Composable
internal fun AutoScrollBar(
    speed: Int,
    paused: Boolean,
    progress: Float?,
    onSpeed: (Int) -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .width(300.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Chrome.bottomBar)
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Row(Modifier.padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Chrome.accent)
                    .clickable(onClickLabel = if (paused) "Resume auto scroll" else "Pause auto scroll", onClick = onPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null, tint = Chrome.onAccent)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(if (paused) "Paused" else "Auto scroll", color = Chrome.onBottomBar, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Speed $speed of 10", color = Chrome.onBottomBarDim, fontSize = 12.sp)
            }
            SmallRoundButton(Icons.Rounded.Remove, "Slower", enabled = speed > 1) { onSpeed(speed - 1) }
            Spacer(Modifier.width(6.dp))
            SmallRoundButton(Icons.Rounded.Add, "Faster", enabled = speed < 10) { onSpeed(speed + 1) }
            IconButton(onClick = onStop) { Icon(Icons.Rounded.Close, "Stop auto scroll", tint = Chrome.onBottomBarDim) }
        }
        if (progress != null && !paused) {
            val track = Chrome.sliderInactive
            val fill = Chrome.accent
            Canvas(Modifier.fillMaxWidth().height(3.dp)) {
                drawRect(track)
                drawRect(fill, size = androidx.compose.ui.geometry.Size(size.width * progress.coerceIn(0f, 1f), size.height))
            }
        }
    }
}

@Composable
private fun SmallRoundButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(Chrome.button).clickable(enabled = enabled, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = if (enabled) Chrome.onBottomBar else Chrome.onBottomBarDim, modifier = Modifier.size(20.dp))
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
