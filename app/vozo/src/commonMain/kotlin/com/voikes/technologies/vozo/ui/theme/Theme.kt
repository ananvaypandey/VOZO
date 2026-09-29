package com.voikes.technologies.vozo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Suppress("ObjectPropertyName")
internal val Color_White = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
@Suppress("ObjectPropertyName")
internal val Color_Black = androidx.compose.ui.graphics.Color(0xFF000000)

private val LightColors = lightColorScheme(
    primary = VozoViolet,
    onPrimary = Color_White,
    primaryContainer = VozoViolet,
    onPrimaryContainer = Color_White,
    secondary = VozoCyan,
    onSecondary = Color_Black,
    tertiary = VozoPurple,
    background = VozoSurface,
    onBackground = VozoOnSurface,
    surface = VozoSurface,
    onSurface = VozoOnSurface,
    surfaceContainer = VozoSurfaceContainer,
    error = VozoError,
)

private val DarkColors = darkColorScheme(
    primary = VozoVioletDark,
    onPrimary = Color_Black,
    primaryContainer = VozoVioletDark,
    onPrimaryContainer = Color_Black,
    secondary = VozoMint,
    tertiary = VozoVioletDark,
    background = VozoSurfaceDark,
    onBackground = VozoOnSurfaceDark,
    surface = VozoSurfaceDark,
    onSurface = VozoOnSurfaceDark,
    surfaceContainer = VozoSurfaceContainerDark,
    error = VozoError,
)

@Composable
fun VozoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}