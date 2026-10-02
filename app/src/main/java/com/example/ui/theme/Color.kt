package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val title: String) {
    OLDED("Olded (Sepia)"),
    PAPER("Paper (Clean White)"),
    MIDNIGHT("Midnight (Pitch OLED)"),
    FOREST("Forest (Sage Dark)"),
    ESPRESSO("Espresso (Mocha Dark)"),
    DUSK("Dusk (Lavender/Indigo)"),
    // Backward compatibility aliases
    LIGHT("Paper (Clean White)"),
    DARK("Midnight (Pitch OLED)"),
    SLATE("Dusk (Lavender/Indigo)");

    companion object {
        fun from(name: String?): ThemeMode {
            if (name == null) return PAPER
            return try {
                when (name.uppercase()) {
                    "LIGHT" -> PAPER
                    "DARK" -> MIDNIGHT
                    "SLATE" -> DUSK
                    else -> valueOf(name)
                }
            } catch (_: Exception) {
                PAPER
            }
        }
    }
}

enum class IconStyle {
    VIBRANT,
    ADAPTIVE
}

object SemanticColors {
    val Folder = Color(0xFFF59E0B)  // Amber
    val Pdf = Color(0xFFEF4444)     // Crimson
    val Target = Color(0xFF10B981)  // Emerald
    val Sync = Color(0xFF0EA5E9)    // Sky Blue
}

@Composable
fun getSemanticIconTint(vibrantColor: Color, style: IconStyle): Color =
    if (style == IconStyle.VIBRANT) vibrantColor else MaterialTheme.colorScheme.onSurfaceVariant

@Immutable
data class MulberryColors(
    val background: Color,
    val surface: Color,
    val surfaceCard: Color,
    val surfaceTint: Color,
    val surfaceBorder: Color,
    val primary: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val borderSubtle: Color,
    val accentDanger: Color,
    val accentSuccess: Color,
    val accentWarning: Color,
    val isDark: Boolean
)

// 1. OLDED (Warm Sepia)
val OldedSepiaBackground = Color(0xFFFBF0D9)
val OldedSepiaSurface = Color(0xFFF4E6C3)
val OldedSepiaSurfaceCard = Color(0xFFF4E6C3)
val OldedSepiaPrimary = Color(0xFFC26D38)
val OldedSepiaTextPrimary = Color(0xFF2C2216)

val MulberryOldedPalette = MulberryColors(
    background = OldedSepiaBackground,
    surface = OldedSepiaSurface,
    surfaceCard = OldedSepiaSurfaceCard,
    surfaceTint = Color(0xFFE8D4A2),
    surfaceBorder = Color(0xFFE8D4A2),
    primary = OldedSepiaPrimary,
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = OldedSepiaTextPrimary,
    textSecondary = Color(0xFF5A4833),
    textMuted = Color(0xFF8C7356),
    borderSubtle = Color(0xFFE8D4A2),
    accentDanger = Color(0xFFC2410C),
    accentSuccess = Color(0xFF047857),
    accentWarning = Color(0xFFB45309),
    isDark = false
)

// 2. PAPER (Clean White)
val PaperBackground = Color(0xFFFFFFFF)
val PaperSurface = Color(0xFFF1F5F9)
val PaperSurfaceCard = Color(0xFFF8FAFC)
val PaperPrimary = Color(0xFF2563EB)
val PaperTextPrimary = Color(0xFF0F172A)

val MulberryPaperPalette = MulberryColors(
    background = PaperBackground,
    surface = PaperSurface,
    surfaceCard = PaperSurfaceCard,
    surfaceTint = Color(0xFFDBEAFE),
    surfaceBorder = Color(0xFFE2E8F0),
    primary = PaperPrimary,
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = PaperTextPrimary,
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    borderSubtle = Color(0xFFCBD5E1),
    accentDanger = Color(0xFFEF4444),
    accentSuccess = Color(0xFF10B981),
    accentWarning = Color(0xFFF59E0B),
    isDark = false
)

// 3. MIDNIGHT (Pitch OLED)
val MidnightBackground = Color(0xFF000000)
val MidnightSurface = Color(0xFF121212)
val MidnightSurfaceCard = Color(0xFF181818)
val MidnightPrimary = Color(0xFF38BDF8)
val MidnightTextPrimary = Color(0xFFF8FAFC)

val MulberryMidnightPalette = MulberryColors(
    background = MidnightBackground,
    surface = MidnightSurface,
    surfaceCard = MidnightSurfaceCard,
    surfaceTint = Color(0xFF0C4A6E),
    surfaceBorder = Color(0xFF27272A),
    primary = MidnightPrimary,
    onPrimary = Color(0xFF082F49),
    textPrimary = MidnightTextPrimary,
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF64748B),
    borderSubtle = Color(0xFF27272A),
    accentDanger = Color(0xFFF87171),
    accentSuccess = Color(0xFF34D399),
    accentWarning = Color(0xFFFBBF24),
    isDark = true
)

// 4. FOREST (Sage Dark)
val ForestBackground = Color(0xFF111A15)
val ForestSurface = Color(0xFF19261F)
val ForestSurfaceCard = Color(0xFF1F3027)
val ForestPrimary = Color(0xFF4ADE80)
val ForestTextPrimary = Color(0xFFECFDF5)

val MulberryForestPalette = MulberryColors(
    background = ForestBackground,
    surface = ForestSurface,
    surfaceCard = ForestSurfaceCard,
    surfaceTint = Color(0xFF14532D),
    surfaceBorder = Color(0xFF233B2E),
    primary = ForestPrimary,
    onPrimary = Color(0xFF052E16),
    textPrimary = ForestTextPrimary,
    textSecondary = Color(0xFFA7F3D0),
    textMuted = Color(0xFF6EE7B7),
    borderSubtle = Color(0xFF233B2E),
    accentDanger = Color(0xFFF87171),
    accentSuccess = Color(0xFF4ADE80),
    accentWarning = Color(0xFFFBBF24),
    isDark = true
)

// 5. ESPRESSO (Mocha Dark)
val EspressoBackground = Color(0xFF1A1412)
val EspressoSurface = Color(0xFF261E1A)
val EspressoSurfaceCard = Color(0xFF302621)
val EspressoPrimary = Color(0xFFF59E0B)
val EspressoTextPrimary = Color(0xFFFEF3C7)

val MulberryEspressoPalette = MulberryColors(
    background = EspressoBackground,
    surface = EspressoSurface,
    surfaceCard = EspressoSurfaceCard,
    surfaceTint = Color(0xFF78350F),
    surfaceBorder = Color(0xFF3D2F28),
    primary = EspressoPrimary,
    onPrimary = Color(0xFF451A03),
    textPrimary = EspressoTextPrimary,
    textSecondary = Color(0xFFFDE68A),
    textMuted = Color(0xFFD97706),
    borderSubtle = Color(0xFF3D2F28),
    accentDanger = Color(0xFFEF4444),
    accentSuccess = Color(0xFF10B981),
    accentWarning = Color(0xFFF59E0B),
    isDark = true
)

// 6. DUSK (Lavender/Indigo)
val DuskBackground = Color(0xFF13111C)
val DuskSurface = Color(0xFF1E1B2E)
val DuskSurfaceCard = Color(0xFF28243D)
val DuskPrimary = Color(0xFFA78BFA)
val DuskTextPrimary = Color(0xFFF5F3FF)

val MulberryDuskPalette = MulberryColors(
    background = DuskBackground,
    surface = DuskSurface,
    surfaceCard = DuskSurfaceCard,
    surfaceTint = Color(0xFF4C1D95),
    surfaceBorder = Color(0xFF352F50),
    primary = DuskPrimary,
    onPrimary = Color(0xFF2E1065),
    textPrimary = DuskTextPrimary,
    textSecondary = Color(0xFFDDD6FE),
    textMuted = Color(0xFF8B5CF6),
    borderSubtle = Color(0xFF352F50),
    accentDanger = Color(0xFFF87171),
    accentSuccess = Color(0xFF34D399),
    accentWarning = Color(0xFFFBBF24),
    isDark = true
)

// Backward compatibility references
val LightCleanBackground = PaperBackground
val LightCleanSurface = PaperSurface
val LightCleanSurfaceCard = PaperSurfaceCard
val LightCleanPrimary = PaperPrimary
val LightCleanOnPrimary = Color(0xFFFFFFFF)
val LightCleanTextPrimary = PaperTextPrimary
val LightCleanTextSecondary = Color(0xFF475569)
val LightCleanTextMuted = Color(0xFF94A3B8)
val LightCleanBorderSubtle = Color(0xFFE2E8F0)
val MulberryLightPalette = MulberryPaperPalette

val DarkOledBackground = MidnightBackground
val DarkOledSurface = MidnightSurface
val DarkOledSurfaceCard = MidnightSurfaceCard
val DarkOledPrimary = MidnightPrimary
val DarkOledOnPrimary = Color(0xFF082F49)
val DarkOledTextPrimary = MidnightTextPrimary
val DarkOledTextSecondary = Color(0xFFCBD5E1)
val DarkOledTextMuted = Color(0xFF64748B)
val DarkOledBorderSubtle = Color(0xFF27272A)
val MulberryDarkPalette = MulberryMidnightPalette

val SlateNightBackground = DuskBackground
val SlateNightSurface = DuskSurface
val SlateNightSurfaceCard = DuskSurfaceCard
val SlateNightPrimary = DuskPrimary
val SlateNightOnPrimary = Color(0xFF2E1065)
val SlateNightTextPrimary = DuskTextPrimary
val SlateNightTextSecondary = Color(0xFFDDD6FE)
val SlateNightTextMuted = Color(0xFF8B5CF6)
val SlateNightBorderSubtle = Color(0xFF352F50)
val MulberrySlatePalette = MulberryDuskPalette

val LocalMulberryColors = staticCompositionLocalOf { MulberryPaperPalette }
