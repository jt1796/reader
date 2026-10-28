package com.reader.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentGold,
    secondary = ShelfWoodHighlight,
    tertiary = AccentGoldLight,
    background = BookshelfBackground,
    surface = BookshelfPanelBg,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight
)

private val LightColorScheme = lightColorScheme(
    primary = ShelfWoodMedium,
    secondary = AccentGold,
    tertiary = ShelfWoodHighlight,
    background = Color(0xFFF9F5F0),
    surface = Color(0xFFFFFDF9),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFF2C1B14),
    onSurface = Color(0xFF2C1B14)
)

@Composable
fun ReaderTheme(
    darkTheme: Boolean = true, // Default to rich wooden bookshelf theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
