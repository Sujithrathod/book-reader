package com.bookreader.app.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookreader.app.BookFormat
import com.bookreader.app.data.DarkPictures
import com.bookreader.app.data.PageFlip
import com.bookreader.app.data.ReaderSettings
import com.bookreader.app.data.ReadingFont
import com.bookreader.app.data.ReadingTheme
import com.bookreader.app.data.RenderQuality
import com.bookreader.app.data.ScreenOrientation
import com.bookreader.app.narration.VoiceSettings

private val MARGIN_COLORS = listOf(
    0xFFC0C0C0 to "Light grey",
    0xFFFFFFFF to "White",
    0xFFE8E2D0 to "Paper",
    0xFF8A8A8A to "Grey",
    0xFF424242 to "Dark grey",
    0xFF000000 to "Black",
)

/**
 * Every reading setting on one page, opened with the Settings button: how the page looks,
 * how pages turn, reading aloud, and (for PDFs) how PDF pages are shown.
 */
@Composable
internal fun ReaderSettingsScreen(
    format: BookFormat,
    s: ReaderSettings,
    onChange: (ReaderSettings) -> Unit,
    voiceLabel: String,
    timerLabel: String,
    onVoices: () -> Unit,
    onTimer: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val isPdf = format == BookFormat.PDF
    val isEpub = format == BookFormat.EPUB

    Column(Modifier.fillMaxSize().background(Chrome.panel).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to the book", tint = Chrome.text) }
            Text("Settings", color = Chrome.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp),
        ) {
            Section("Display") {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                    Label("Page colours")
                    Hint("Background and text colours chosen for comfortable reading.")
                }
                ThemeRow(s.theme, { onChange(s.copy(theme = it)) }, Modifier.padding(top = 8.dp, bottom = 10.dp))
                Line()
                SwitchRow("Use phone brightness", "Turn off to set a brightness just for reading.", s.brightness == null) { phone ->
                    onChange(s.copy(brightness = if (phone) null else 0.5f))
                }
                s.brightness?.let { b ->
                    SliderRow("Brightness", "${(b * 100).toInt()}%", b, ReaderSettings.BRIGHTNESS_RANGE) { onChange(s.copy(brightness = it)) }
                }
                SliderRow("Warm light", if (s.warmth <= 0f) "Off" else "${(s.warmth * 100).toInt()}%", s.warmth, 0f..1f) { onChange(s.copy(warmth = it)) }
                Line()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Label("Text size")
                        if (isPdf) Hint("Used in text reflow (the \"A\" button).")
                    }
                    StepButton("A−", "Smaller text") { onChange(s.copy(fontScale = (s.fontScale - 0.1).coerceIn(ReaderSettings.FONT_RANGE))) }
                    Text("${Math.round(s.fontScale * 100)}%", color = Chrome.text, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 12.dp))
                    StepButton("A+", "Larger text") { onChange(s.copy(fontScale = (s.fontScale + 0.1).coerceIn(ReaderSettings.FONT_RANGE))) }
                }
                Line()
                ChoiceRow("Font", ReadingFont.entries, s.font, { it.label }) { onChange(s.copy(font = it)) }
                Line()
                SliderRow(
                    "Line spacing", "%.1f".format(s.lineHeight), s.lineHeight.toFloat(),
                    ReaderSettings.LINE_RANGE.start.toFloat()..ReaderSettings.LINE_RANGE.endInclusive.toFloat(), steps = 9,
                ) { onChange(s.copy(lineHeight = it.toDouble())) }
                SliderRow(
                    "Margins", "%.1f".format(s.pageMargins), s.pageMargins.toFloat(),
                    ReaderSettings.MARGIN_RANGE.start.toFloat()..ReaderSettings.MARGIN_RANGE.endInclusive.toFloat(), steps = 5,
                ) { onChange(s.copy(pageMargins = it.toDouble())) }
                if (s.theme.dark && format != BookFormat.TXT) {
                    Line()
                    ChoiceRow(
                        if (isPdf) "Pages in dark mode" else "Pictures in dark mode",
                        DarkPictures.entries, if (isPdf) s.darkPdfPages else s.darkImages, { it.label },
                    ) { onChange(if (isPdf) s.copy(darkPdfPages = it) else s.copy(darkImages = it)) }
                }
                Line()
                SwitchRow("Status line", "Battery, time, chapter and page under the text.", s.statusLine) { onChange(s.copy(statusLine = it)) }
            }

            Section("Page turning") {
                if (isEpub) {
                    SwitchRow("Scroll instead of turning pages", null, s.scroll) { onChange(s.copy(scroll = it)) }
                    Line()
                }
                if (isPdf) {
                    ChoiceRow("Page layout", listOf(false, true), s.pdfPaged, { if (it) "Page by page" else "Continuous" }) { onChange(s.copy(pdfPaged = it)) }
                    Line()
                }
                ChoiceRow("Page turn animation", PageFlip.entries, s.pageFlip, { it.label }) { onChange(s.copy(pageFlip = it)) }
                Line()
                SwitchRow("Tap sides to turn pages", "The middle of the page always opens the menu.", s.tapEdgesTurn) { onChange(s.copy(tapEdgesTurn = it)) }
                Line()
                SwitchRow("Volume buttons turn pages", "Not while a book is being read aloud.", s.volumeKeysTurn) { onChange(s.copy(volumeKeysTurn = it)) }
                Line()
                SliderRow(
                    "Auto scroll speed", "${s.autoScrollSpeed}", s.autoScrollSpeed.toFloat(),
                    ReaderSettings.AUTO_SCROLL_RANGE.first.toFloat()..ReaderSettings.AUTO_SCROLL_RANGE.last.toFloat(), steps = 8,
                ) { onChange(s.copy(autoScrollSpeed = Math.round(it))) }
                Line()
                ChoiceRow("Screen rotation", ScreenOrientation.entries, s.orientation, { it.short }) { onChange(s.copy(orientation = it)) }
                Line()
                SwitchRow("Keep screen on while reading", null, s.keepScreenOn) { onChange(s.copy(keepScreenOn = it)) }
            }

            Section("Read aloud") {
                LinkRow("Voice", voiceLabel, onVoices)
                Line()
                SliderRow("Speed", "%.1f×".format(s.voice.rate), s.voice.rate, VoiceSettings.RATE_RANGE, steps = 19) {
                    onChange(s.copy(voice = s.voice.copy(rate = it)))
                }
                SliderRow("Pitch", "%.1f".format(s.voice.pitch), s.voice.pitch, VoiceSettings.PITCH_RANGE, steps = 14) {
                    onChange(s.copy(voice = s.voice.copy(pitch = it)))
                }
                Line()
                SwitchRow(
                    "Long-press a word to read at once",
                    "Off: long-press shows a menu with \"Read from here\" and \"Copy\".",
                    s.longPressReads,
                ) { onChange(s.copy(longPressReads = it)) }
                Line()
                LinkRow("Sleep timer", timerLabel, onTimer)
            }

            if (isPdf) {
                Section("PDF pages") {
                    SwitchRow("Trim white page margins", "Makes the text larger.", s.pdfCropMargins) { onChange(s.copy(pdfCropMargins = it)) }
                    Line()
                    ChoiceRow("Sharpness", RenderQuality.entries, s.pdfQuality, { it.label }) { onChange(s.copy(pdfQuality = it)) }
                    Line()
                    SliderRow(
                        "Gap between pages", "${s.pdfPageGap}", s.pdfPageGap.toFloat(),
                        ReaderSettings.PAGE_GAP_RANGE.first.toFloat()..ReaderSettings.PAGE_GAP_RANGE.last.toFloat(),
                    ) { onChange(s.copy(pdfPageGap = it.toInt())) }
                    Line()
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Label("Colour around pages")
                        Hint("Light and sepia themes; the dark theme stays black.")
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MARGIN_COLORS.forEach { (argb, name) ->
                                val selected = s.pdfMarginColor == argb.toInt()
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb.toInt()))
                                        .border(if (selected) 3.dp else 1.dp, if (selected) Chrome.accent else Chrome.divider, CircleShape)
                                        .clickable(onClickLabel = name) { onChange(s.copy(pdfMarginColor = argb.toInt())) },
                                )
                            }
                        }
                    }
                    Line()
                    SwitchRow("Text reflow button on the top bar", null, s.reflowButton) { onChange(s.copy(reflowButton = it)) }
                    Line()
                    SwitchRow("Page thumbnails in the menu", null, s.thumbnails) { onChange(s.copy(thumbnails = it)) }
                }
            }
        }
    }
}

private val ScreenOrientation.short: String
    get() = when (this) {
        ScreenOrientation.AUTO -> "Auto"
        ScreenOrientation.PORTRAIT -> "Portrait"
        ScreenOrientation.LANDSCAPE -> "Landscape"
    }

// ---------- Building blocks ----------

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, color = Chrome.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Chrome.card).padding(vertical = 4.dp), content = content)
}

@Composable
private fun Line() = HorizontalDivider(color = Chrome.divider, modifier = Modifier.padding(horizontal = 16.dp))

@Composable
private fun Label(text: String) = Text(text, color = Chrome.text, fontSize = 16.sp)

@Composable
private fun Hint(text: String) = Text(text, color = Chrome.textDim, fontSize = 13.sp, lineHeight = 17.sp)

@Composable
private fun SwitchRow(label: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Label(label)
            if (hint != null) Hint(hint)
        }
        Switch(checked = checked, onCheckedChange = null, colors = chromeSwitchColors())
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: String,
    position: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { Label(label) }
            Text(value, color = Chrome.textDim, fontSize = 14.sp)
        }
        ThinSlider(value = position, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

/** A label with its choices under it as a row of pills; the chosen one is filled. */
@Composable
private fun <T> ChoiceRow(label: String, options: List<T>, selected: T, name: (T) -> String, onPick: (T) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Label(label)
        Row(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(14.dp)).background(Chrome.button).padding(3.dp)) {
            options.forEach { o ->
                val on = o == selected
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (on) Chrome.accent else Color.Transparent)
                        .clickable(role = Role.RadioButton) { onPick(o) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        name(o),
                        color = if (on) Chrome.onAccent else Chrome.text,
                        fontSize = 14.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** A row that opens something else, showing its current value. */
@Composable
private fun LinkRow(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Label(label)
        Text(
            value,
            color = Chrome.textDim,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp, end = 4.dp),
        )
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Chrome.textDim)
    }
}

@Composable
private fun StepButton(text: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 52.dp, height = 40.dp).clip(RoundedCornerShape(12.dp)).background(Chrome.button).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Chrome.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
