package com.bookreader.app.reader

import androidx.compose.ui.graphics.Color
import com.bookreader.app.data.ReadingTheme

/** Page colours of a reading theme, as Compose colours. */
data class ReaderColors(val background: Color, val text: Color)

fun ReadingTheme.colors(): ReaderColors = ReaderColors(Color(background.toInt()), Color(text.toInt()))
