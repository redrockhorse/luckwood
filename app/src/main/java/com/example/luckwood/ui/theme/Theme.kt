package com.example.luckwood.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BallRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5C1A1A),
    onPrimaryContainer = BallRedSoft,
    secondary = BallBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0D3A6E),
    onSecondaryContainer = BallBlueSoft,
    tertiary = BallBlue,
    background = InkDark,
    onBackground = OnInk,
    surface = SurfaceDark,
    onSurface = OnInk,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = Color(0xFFB8B4AC),
    outline = Color(0xFF6B6760),
    error = BallRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BallRed,
    onPrimary = Color.White,
    primaryContainer = BallRedSoft,
    onPrimaryContainer = Color(0xFF3E0A0A),
    secondary = BallBlue,
    onSecondary = Color.White,
    secondaryContainer = BallBlueSoft,
    onSecondaryContainer = Color(0xFF0A2A4A),
    tertiary = BallBlue,
    background = PaperBackground,
    onBackground = InkText,
    surface = SurfaceLift,
    onSurface = InkText,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = Color(0xFF5C574F),
    outline = Color(0xFFB0A99C),
    error = BallRed,
    onError = Color.White
)

@Composable
fun LuckwoodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Brand palette only — disable Material You dynamic purple takeover
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
