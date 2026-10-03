package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AishaDarkColorScheme = darkColorScheme(
    primary = AishaCyan,
    onPrimary = Color.Black,
    primaryContainer = AishaViolet.copy(alpha = 0.35f),
    onPrimaryContainer = AishaCyan,
    secondary = AishaMagenta,
    onSecondary = Color.White,
    secondaryContainer = AishaMagenta.copy(alpha = 0.25f),
    onSecondaryContainer = AishaMagenta,
    tertiary = AishaPurple,
    onTertiary = Color.White,
    background = AishaDarkBackground,
    onBackground = AishaTextPrimary,
    surface = AishaSurface,
    onSurface = AishaTextPrimary,
    surfaceVariant = AishaSurfaceVariant,
    onSurfaceVariant = AishaTextSecondary,
    outline = AishaBorder,
    outlineVariant = AishaBorderHighlight
)

@Composable
fun AishaTheme(
    darkTheme: Boolean = true, // Aisha is styled with a sleek futuristic dark glass UI by default
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AishaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
