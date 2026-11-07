package com.bookreader.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Pine = Color(0xFF2F5D50)
private val PineLight = Color(0xFF8CC7B5)
private val Amber = Color(0xFFF2B84B)
private val Paper = Color(0xFFFBF8F1)
private val Ink = Color(0xFF1D2433)

private val LightColors = lightColorScheme(
    primary = Pine,
    onPrimary = Color.White,
    secondary = Amber,
    onSecondary = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = PineLight,
    onPrimary = Color(0xFF00382C),
    secondary = Amber,
    onSecondary = Ink,
    background = Color(0xFF121614),
    surface = Color(0xFF121614),
)

/** App chrome theme. Reading themes (light, dark, sepia) for book pages come in step 2. */
@Composable
fun BookReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
