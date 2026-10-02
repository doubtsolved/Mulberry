package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val MulberryShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

object MulberryTheme {
    val colors: MulberryColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMulberryColors.current

    val typography = MulberryTypography
    val shapes = MulberryShapes
}

@Composable
fun MulberryTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val mulberryPalette = when (themeMode) {
        ThemeMode.OLDED -> MulberryOldedPalette
        ThemeMode.PAPER, ThemeMode.LIGHT -> MulberryPaperPalette
        ThemeMode.MIDNIGHT, ThemeMode.DARK -> MulberryMidnightPalette
        ThemeMode.FOREST -> MulberryForestPalette
        ThemeMode.ESPRESSO -> MulberryEspressoPalette
        ThemeMode.DUSK, ThemeMode.SLATE -> MulberryDuskPalette
    }

    val materialColorScheme = if (mulberryPalette.isDark) {
        darkColorScheme(
            primary = mulberryPalette.primary,
            onPrimary = mulberryPalette.onPrimary,
            background = mulberryPalette.background,
            surface = mulberryPalette.surface,
            surfaceVariant = mulberryPalette.surfaceCard,
            onBackground = mulberryPalette.textPrimary,
            onSurface = mulberryPalette.textPrimary,
            onSurfaceVariant = mulberryPalette.textSecondary,
            outline = mulberryPalette.borderSubtle
        )
    } else {
        lightColorScheme(
            primary = mulberryPalette.primary,
            onPrimary = mulberryPalette.onPrimary,
            background = mulberryPalette.background,
            surface = mulberryPalette.surface,
            surfaceVariant = mulberryPalette.surfaceCard,
            onBackground = mulberryPalette.textPrimary,
            onSurface = mulberryPalette.textPrimary,
            onSurfaceVariant = mulberryPalette.textSecondary,
            outline = mulberryPalette.borderSubtle
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = mulberryPalette.background.toArgb()
                window.navigationBarColor = mulberryPalette.surface.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !mulberryPalette.isDark
                insetsController.isAppearanceLightNavigationBars = !mulberryPalette.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalMulberryColors provides mulberryPalette
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = MulberryTypography,
            shapes = MulberryShapes,
            content = content
        )
    }
}
