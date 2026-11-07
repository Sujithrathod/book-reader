package com.bookreader.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ReadingTheme(val label: String) { LIGHT("Light"), DARK("Dark"), SEPIA("Sepia") }

/** How pictures (EPUB images, whole PDF pages) are shown with the dark theme. */
enum class DarkPictures(val label: String) { DIM("Dim"), INVERT("Invert"), ORIGINAL("Original") }

/** PDF page sharpness: rendered width as a multiple of the screen width. */
enum class RenderQuality(val label: String, val scale: Float) { LOW("Low", 0.75f), MEDIUM("Medium", 1f), HIGH("High", 1.5f) }

/** Animation when a tap or button turns the page. */
enum class PageFlip(val label: String) { NONE("None"), SLIDE("Slide") }

enum class ScreenOrientation(val label: String) { AUTO("Auto-rotate"), PORTRAIT("Portrait"), LANDSCAPE("Landscape") }

/** Reading look, shared by every book. Ranges are kept inside what Readium accepts. */
data class ReaderSettings(
    val theme: ReadingTheme = ReadingTheme.LIGHT,
    val fontScale: Double = 1.0,      // 0.75 .. 2.5
    val lineHeight: Double = 1.5,     // 1.0 .. 2.0
    val pageMargins: Double = 1.0,    // 0.5 .. 2.0
    val scroll: Boolean = false,      // EPUB: scroll instead of pages
    /** true: long-press a word starts reading at once. false: shows a menu with "Read from here". */
    val longPressReads: Boolean = true,
    /** Dark theme: EPUB pictures get the same night look as PDF pages (see darkPdfPages). */
    val darkImages: DarkPictures = DarkPictures.INVERT,
    /** Dark theme: PDF pages are colour-inverted as in Moon+ Reader (white paper turns black, blue turns yellow); pictures that are already dark stay dark. */
    val darkPdfPages: DarkPictures = DarkPictures.INVERT,
    val voice: com.bookreader.app.narration.VoiceSettings = com.bookreader.app.narration.VoiceSettings(),
    /** PDF: colour around and between pages (light themes; the dark theme uses black). */
    val pdfMarginColor: Int = DEFAULT_MARGIN_COLOR,
    val pdfQuality: RenderQuality = RenderQuality.MEDIUM,
    /** PDF: space between pages in dp. */
    val pdfPageGap: Int = 8,
    /** PDF: one page at a time, turned sideways, instead of a continuous scroll. */
    val pdfPaged: Boolean = false,
    /** PDF: trims white page borders so the text is larger. */
    val pdfCropMargins: Boolean = false,
    /** PDF: shows the text-reflow ("A") button on the top bar. */
    val reflowButton: Boolean = true,
    /** PDF: page thumbnails above the page slider. */
    val thumbnails: Boolean = false,
    val pageFlip: PageFlip = PageFlip.NONE,
    val orientation: ScreenOrientation = ScreenOrientation.AUTO,
    /** Tapping the page edges turns pages; the middle always opens the menu. */
    val tapEdgesTurn: Boolean = true,
    val keepScreenOn: Boolean = true,
    /** The line under the page with battery, time, chapter and page number. */
    val statusLine: Boolean = true,
    val autoScrollSpeed: Int = 3,     // 1 .. 10
) {
    companion object {
        val FONT_RANGE = 0.75..2.5
        val LINE_RANGE = 1.0..2.0
        val MARGIN_RANGE = 0.5..2.0
        val PAGE_GAP_RANGE = 0..32
        val AUTO_SCROLL_RANGE = 1..10
        const val DEFAULT_MARGIN_COLOR = 0xFFC0C0C0.toInt()
    }
}

private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
    name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default

private val Context.settingsStore by preferencesDataStore(name = "reader_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val fontScale = doublePreferencesKey("font_scale")
        val lineHeight = doublePreferencesKey("line_height")
        val pageMargins = doublePreferencesKey("page_margins")
        val scroll = booleanPreferencesKey("scroll")
        val longPressReads = booleanPreferencesKey("long_press_reads")
        // Renamed from "dark_images" when Invert became the default, so earlier saved "Dim" values reset.
        val darkImages = stringPreferencesKey("night_images")
        val darkPdfPages = stringPreferencesKey("dark_pdf_pages")
        val voiceName = stringPreferencesKey("voice_name")
        val rate = floatPreferencesKey("speech_rate")
        val pitch = floatPreferencesKey("speech_pitch")
        val pdfMarginColor = intPreferencesKey("pdf_margin_color")
        val pdfQuality = stringPreferencesKey("pdf_quality")
        val pdfPageGap = intPreferencesKey("pdf_page_gap")
        val pdfPaged = booleanPreferencesKey("pdf_paged")
        val pdfCropMargins = booleanPreferencesKey("pdf_crop_margins")
        val reflowButton = booleanPreferencesKey("reflow_button")
        val thumbnails = booleanPreferencesKey("thumbnails")
        val pageFlip = stringPreferencesKey("page_flip")
        val orientation = stringPreferencesKey("orientation")
        val tapEdgesTurn = booleanPreferencesKey("tap_edges_turn")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val statusLine = booleanPreferencesKey("status_line")
        val autoScrollSpeed = intPreferencesKey("auto_scroll_speed")
    }

    val settings: Flow<ReaderSettings> = context.settingsStore.data.map { p ->
        ReaderSettings(
            theme = p[Keys.theme]?.let { runCatching { ReadingTheme.valueOf(it) }.getOrNull() } ?: ReadingTheme.LIGHT,
            fontScale = p[Keys.fontScale] ?: 1.0,
            lineHeight = p[Keys.lineHeight] ?: 1.5,
            pageMargins = p[Keys.pageMargins] ?: 1.0,
            scroll = p[Keys.scroll] ?: false,
            longPressReads = p[Keys.longPressReads] ?: true,
            darkImages = p[Keys.darkImages]?.let { runCatching { DarkPictures.valueOf(it) }.getOrNull() } ?: DarkPictures.INVERT,
            darkPdfPages = p[Keys.darkPdfPages]?.let { runCatching { DarkPictures.valueOf(it) }.getOrNull() } ?: DarkPictures.INVERT,
            voice = com.bookreader.app.narration.VoiceSettings(
                voiceName = p[Keys.voiceName],
                rate = p[Keys.rate] ?: 1.0f,
                pitch = p[Keys.pitch] ?: 1.0f,
            ),
            pdfMarginColor = p[Keys.pdfMarginColor] ?: ReaderSettings.DEFAULT_MARGIN_COLOR,
            pdfQuality = enumOr(p[Keys.pdfQuality], RenderQuality.MEDIUM),
            pdfPageGap = p[Keys.pdfPageGap] ?: 8,
            pdfPaged = p[Keys.pdfPaged] ?: false,
            pdfCropMargins = p[Keys.pdfCropMargins] ?: false,
            reflowButton = p[Keys.reflowButton] ?: true,
            thumbnails = p[Keys.thumbnails] ?: false,
            pageFlip = enumOr(p[Keys.pageFlip], PageFlip.NONE),
            orientation = enumOr(p[Keys.orientation], ScreenOrientation.AUTO),
            tapEdgesTurn = p[Keys.tapEdgesTurn] ?: true,
            keepScreenOn = p[Keys.keepScreenOn] ?: true,
            statusLine = p[Keys.statusLine] ?: true,
            autoScrollSpeed = p[Keys.autoScrollSpeed] ?: 3,
        )
    }

    suspend fun update(s: ReaderSettings) {
        context.settingsStore.edit { p ->
            p[Keys.theme] = s.theme.name
            p[Keys.fontScale] = s.fontScale.coerceIn(ReaderSettings.FONT_RANGE)
            p[Keys.lineHeight] = s.lineHeight.coerceIn(ReaderSettings.LINE_RANGE)
            p[Keys.pageMargins] = s.pageMargins.coerceIn(ReaderSettings.MARGIN_RANGE)
            p[Keys.scroll] = s.scroll
            p[Keys.longPressReads] = s.longPressReads
            p[Keys.darkImages] = s.darkImages.name
            p[Keys.darkPdfPages] = s.darkPdfPages.name
            s.voice.voiceName?.let { p[Keys.voiceName] = it } ?: p.remove(Keys.voiceName)
            p[Keys.rate] = s.voice.rate.coerceIn(com.bookreader.app.narration.VoiceSettings.RATE_RANGE)
            p[Keys.pitch] = s.voice.pitch.coerceIn(com.bookreader.app.narration.VoiceSettings.PITCH_RANGE)
            p[Keys.pdfMarginColor] = s.pdfMarginColor
            p[Keys.pdfQuality] = s.pdfQuality.name
            p[Keys.pdfPageGap] = s.pdfPageGap.coerceIn(ReaderSettings.PAGE_GAP_RANGE)
            p[Keys.pdfPaged] = s.pdfPaged
            p[Keys.pdfCropMargins] = s.pdfCropMargins
            p[Keys.reflowButton] = s.reflowButton
            p[Keys.thumbnails] = s.thumbnails
            p[Keys.pageFlip] = s.pageFlip.name
            p[Keys.orientation] = s.orientation.name
            p[Keys.tapEdgesTurn] = s.tapEdgesTurn
            p[Keys.keepScreenOn] = s.keepScreenOn
            p[Keys.statusLine] = s.statusLine
            p[Keys.autoScrollSpeed] = s.autoScrollSpeed.coerceIn(ReaderSettings.AUTO_SCROLL_RANGE)
        }
    }
}
