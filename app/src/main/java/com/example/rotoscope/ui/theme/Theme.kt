package com.example.rotoscope.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AccentTeal,
    onPrimary = SurfaceDark,
    secondary = AccentTealDark,
    background = SurfaceDark,
    surface = SurfaceDarkElevated,
    error = ErrorRed,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val LightColors = lightColorScheme(
    primary = AccentTealDark,
    secondary = AccentTeal,
    error = ErrorRed
)

@Composable
fun RotoscopeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RotoscopeTypography,
        content = content
    )
}
