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
    // Buttons, chips and sheets in soft greens, so nothing falls back to Material's default purple.
    primaryContainer = Color(0xFFCFE6DC),
    onPrimaryContainer = Color(0xFF12362C),
    secondaryContainer = Color(0xFFDCEBE4),
    onSecondaryContainer = Color(0xFF12362C),
    tertiary = Pine,
    surfaceVariant = Color(0xFFE9EDE7),
    onSurfaceVariant = Color(0xFF56615B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F3EB),
    surfaceContainer = Color(0xFFF3F0E8),
    surfaceContainerHigh = Color(0xFFEEEBE2),
    surfaceContainerHighest = Color(0xFFE8E5DC),
    outline = Color(0xFF8A958E),
    outlineVariant = Color(0xFFCDD5CF),
)

private val DarkColors = darkColorScheme(
    primary = PineLight,
    onPrimary = Color(0xFF00382C),
    secondary = Amber,
    onSecondary = Ink,
    background = Color(0xFF121614),
    surface = Color(0xFF121614),
    primaryContainer = Color(0xFF244A3F),
    onPrimaryContainer = Color(0xFFCFE6DC),
    secondaryContainer = Color(0xFF2A3A34),
    onSecondaryContainer = Color(0xFFDCEBE4),
    tertiary = PineLight,
    surfaceVariant = Color(0xFF252B28),
    onSurfaceVariant = Color(0xFFB5C0BA),
    surfaceContainerLowest = Color(0xFF0D100F),
    surfaceContainerLow = Color(0xFF181D1B),
    surfaceContainer = Color(0xFF1C2220),
    surfaceContainerHigh = Color(0xFF242A28),
    surfaceContainerHighest = Color(0xFF2D3431),
    outline = Color(0xFF7F8A84),
    outlineVariant = Color(0xFF3A423E),
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
