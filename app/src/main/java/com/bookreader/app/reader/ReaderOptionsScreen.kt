package com.bookreader.app.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookreader.app.data.PageFlip
import com.bookreader.app.data.ReaderSettings
import com.bookreader.app.data.RenderQuality

private val MARGIN_COLORS = listOf(
    0xFFC0C0C0 to "Light grey",
    0xFFFFFFFF to "White",
    0xFFE8E2D0 to "Paper",
    0xFF8A8A8A to "Grey",
    0xFF424242 to "Dark grey",
    0xFF000000 to "Black",
)

/**
 * Full-screen options page in the style of Moon+ Reader's "PDF Options": grey rounded cards
 * with right-aligned labels. Only options that do something in this app are listed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReaderOptionsScreen(isPdf: Boolean, s: ReaderSettings, onChange: (ReaderSettings) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    var dialog by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(Chrome.panel).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBackIos, "Back to the book", tint = Chrome.text) }
            Text(if (isPdf) "PDF Options" else "Reading Options", color = Chrome.text, fontSize = 26.sp)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (isPdf) {
                OptionCard {
                    OptionRow("Margin Color") {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(s.pdfMarginColor))
                                .clickable(onClickLabel = "Choose margin colour") { dialog = "color" },
                        )
                    }
                    OptionRow("Render Quality") {
                        ChoiceDropdown(RenderQuality.entries, s.pdfQuality, { it.label }) { onChange(s.copy(pdfQuality = it)) }
                    }
                    OptionRow("Page Gap") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ThinSlider(
                                value = s.pdfPageGap.toFloat(),
                                onValueChange = { onChange(s.copy(pdfPageGap = it.toInt())) },
                                valueRange = ReaderSettings.PAGE_GAP_RANGE.first.toFloat()..ReaderSettings.PAGE_GAP_RANGE.last.toFloat(),
                                modifier = Modifier.weight(1f),
                            )
                            StepButton("−", "Smaller page gap") { onChange(s.copy(pdfPageGap = (s.pdfPageGap - 2).coerceIn(ReaderSettings.PAGE_GAP_RANGE))) }
                            StepButton("+", "Larger page gap") { onChange(s.copy(pdfPageGap = (s.pdfPageGap + 2).coerceIn(ReaderSettings.PAGE_GAP_RANGE))) }
                        }
                    }
                }
                OptionCard {
                    SwitchRow("Text Reflow button on top bar", s.reflowButton) { onChange(s.copy(reflowButton = it)) }
                    SwitchRow("Thumbnail Previews on bottom bar", s.thumbnails) { onChange(s.copy(thumbnails = it)) }
                    SwitchRow("Trim white page margins", s.pdfCropMargins) { onChange(s.copy(pdfCropMargins = it)) }
                }
            }
            OptionCard {
                OptionRow("Page Flip Animation", labelWeight = 1.7f) {
                    ChoiceDropdown(PageFlip.entries, s.pageFlip, { it.label }) { onChange(s.copy(pageFlip = it)) }
                }
            }
            if (isPdf) {
                OptionCard {
                    Text("Page layout", color = Chrome.text, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        RadioChoice("Continuous", !s.pdfPaged) { onChange(s.copy(pdfPaged = false)) }
                        RadioChoice("Page by page", s.pdfPaged) { onChange(s.copy(pdfPaged = true)) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                PillButton("Control Options", Modifier.weight(1f)) { dialog = "control" }
                PillButton("Miscellaneous", Modifier.weight(1f)) { dialog = "misc" }
            }
        }
    }

    when (dialog) {
        "color" -> OptionsDialog("Margin Color", onDismiss = { dialog = null }) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MARGIN_COLORS.forEach { (argb, name) ->
                    val selected = s.pdfMarginColor == argb.toInt()
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(argb.toInt()))
                            .border(if (selected) 4.dp else 1.dp, if (selected) Chrome.accent else Chrome.divider, CircleShape)
                            .clickable(onClickLabel = name) { onChange(s.copy(pdfMarginColor = argb.toInt())); dialog = null },
                    )
                }
            }
            Text("Used with the light and sepia themes. The dark theme keeps black margins.", color = Chrome.textDim, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        }
        "control" -> OptionsDialog("Control Options", onDismiss = { dialog = null }) {
            SwitchRow("Tap left or right side to turn pages", s.tapEdgesTurn) { onChange(s.copy(tapEdgesTurn = it)) }
            Text("Tap the middle of the page to show or hide the menu.", color = Chrome.textDim, fontSize = 13.sp)
        }
        "misc" -> OptionsDialog("Miscellaneous", onDismiss = { dialog = null }) {
            SwitchRow("Keep screen on while reading", s.keepScreenOn) { onChange(s.copy(keepScreenOn = it)) }
            SwitchRow("Status line (battery, time, chapter, page)", s.statusLine) { onChange(s.copy(statusLine = it)) }
        }
    }
}

@Composable
private fun OptionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Chrome.card).padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

/** Label on the left, right-aligned against its control, as on Moon+ Reader's options pages. */
@Composable
private fun OptionRow(label: String, labelWeight: Float = 1f, control: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Chrome.text, fontSize = 18.sp, textAlign = TextAlign.End, modifier = Modifier.weight(labelWeight).padding(end = 16.dp))
        Box(Modifier.weight(1.25f)) { control() }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Chrome.text, fontSize = 18.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f).padding(end = 16.dp))
        Switch(checked = checked, onCheckedChange = onChange, colors = chromeSwitchColors())
    }
}

@Composable
private fun <T> ChoiceDropdown(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Column(Modifier.fillMaxWidth().clickable { open = true }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp)) {
                Text(label(selected), color = Chrome.textDim, fontSize = 18.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, null, tint = Chrome.textDim)
            }
            HorizontalDivider(color = Chrome.divider)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(label(o), color = Chrome.text) }, onClick = { open = false; onPick(o) })
            }
        }
    }
}

@Composable
private fun RadioChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.clickable(onClick = onClick).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Chrome.accent, unselectedColor = Chrome.textDim))
        Text(label, color = Chrome.text, fontSize = 18.sp)
    }
}

@Composable
private fun StepButton(text: String, label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.width(44.dp).semantics { contentDescription = label }) {
        Text(text, color = Chrome.accent, fontSize = 24.sp)
    }
}

@Composable
private fun PillButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(26.dp)).background(Chrome.button).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Chrome.text, fontSize = 17.sp)
    }
}

@Composable
private fun OptionsDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Chrome.panel,
        title = { Text(title, color = Chrome.text) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = Chrome.accent) } },
    )
}
