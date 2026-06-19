package ru.ugrasu.eljunior.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryRed,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryRedLight,
    onPrimaryContainer = PrimaryRedDark,
    secondary = AccentOrange,
    onSecondary = TextOnPrimary,
    background = BackgroundGray,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    error = ErrorColor,
    onError = TextOnPrimary,
    outline = DividerColor,
    surfaceVariant = BackgroundWhite,
    onSurfaceVariant = TextSecondary
)

@Composable
fun ELJuniorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Always use light theme for this app

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
