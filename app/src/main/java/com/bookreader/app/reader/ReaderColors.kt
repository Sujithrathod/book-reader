package com.bookreader.app.reader

import androidx.compose.ui.graphics.Color
import com.bookreader.app.data.ReadingTheme

/** Page colours. These match Readium's own light, dark and sepia themes so all formats look alike. */
data class ReaderColors(val background: Color, val text: Color, val chrome: Color)

fun ReadingTheme.colors(): ReaderColors = when (this) {
    ReadingTheme.LIGHT -> ReaderColors(Color(0xFFFFFFFF), Color(0xFF121212), Color(0xFFF2F2F2))
    ReadingTheme.DARK -> ReaderColors(Color(0xFF000000), Color(0xFFFEFEFE), Color(0xFF1A1A1A))
    ReadingTheme.SEPIA -> ReaderColors(Color(0xFFFAF4E8), Color(0xFF121212), Color(0xFFEFE6D2))
}
