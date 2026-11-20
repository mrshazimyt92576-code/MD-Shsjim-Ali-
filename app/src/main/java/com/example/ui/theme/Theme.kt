package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.data.model.ThemeMode

@Immutable
data class LiquidGlassColors(
    val isDark: Boolean,
    val backdropBase: Color,
    val glassFill: Color,
    val glassFillStrong: Color,
    val borderHighlight: Color,
    val borderSubtle: Color,
    val accentCyan: Color,
    val accentGold: Color,
    val accentEmerald: Color,
    val accentCoral: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

val LocalLiquidGlassColors = staticCompositionLocalOf {
    LiquidGlassColors(
        isDark = true,
        backdropBase = ObsidianVoid,
        glassFill = GlassFillDark,
        glassFillStrong = GlassFillDarkStrong,
        borderHighlight = GlassBorderHighlight,
        borderSubtle = GlassBorderSubtle,
        accentCyan = HdrCyan,
        accentGold = HdrGold,
        accentEmerald = HdrEmerald,
        accentCoral = HdrCoral,
        textPrimary = TextPrimaryDark,
        textSecondary = TextSecondaryDark
    )
}

private val ObsidianColorScheme = darkColorScheme(
    primary = HdrCyan,
    onPrimary = Color(0xFF002029),
    primaryContainer = Color(0xFF0B3242),
    onPrimaryContainer = HdrCyan,
    secondary = HdrGold,
    onSecondary = Color(0xFF281800),
    secondaryContainer = Color(0xFF3A280A),
    onSecondaryContainer = HdrGold,
    tertiary = HdrEmerald,
    background = ObsidianVoid,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondaryDark,
    error = HdrCoral,
    onError = Color(0xFF2E0007),
    outline = GlassBorderSubtle
)

private val MidnightColorScheme = darkColorScheme(
    primary = HdrCyan,
    onPrimary = Color(0xFF001E28),
    primaryContainer = Color(0xFF103854),
    onPrimaryContainer = HdrCyan,
    secondary = HdrGold,
    onSecondary = Color(0xFF281800),
    background = MidnightVoid,
    onBackground = TextPrimaryDark,
    surface = MidnightSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF1B2846),
    onSurfaceVariant = TextSecondaryDark,
    error = HdrCoral,
    outline = GlassBorderSubtle
)

private val LightColorScheme = lightColorScheme(
    primary = HdrCyanDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F5FF),
    onPrimaryContainer = Color(0xFF002B3A),
    secondary = HdrGoldDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFECCB),
    onSecondaryContainer = Color(0xFF331D00),
    tertiary = Color(0xFF00945E),
    background = LightFrostVoid,
    onBackground = TextPrimaryLight,
    surface = LightFrostSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE1E9F6),
    onSurfaceVariant = TextSecondaryLight,
    error = Color(0xFFCF2E43),
    outline = GlassBorderLightSubtle
)

private val HdrStudioShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun HdrStudiosTheme(
    themeMode: ThemeMode = ThemeMode.OBSIDIAN,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.OBSIDIAN, ThemeMode.MIDNIGHT -> true
        ThemeMode.SYSTEM -> systemDark
    }

    val colorScheme = when {
        themeMode == ThemeMode.MIDNIGHT -> MidnightColorScheme
        useDark -> ObsidianColorScheme
        else -> LightColorScheme
    }

    val glassColors = if (useDark) {
        LiquidGlassColors(
            isDark = true,
            backdropBase = if (themeMode == ThemeMode.MIDNIGHT) MidnightVoid else ObsidianVoid,
            glassFill = GlassFillDark,
            glassFillStrong = GlassFillDarkStrong,
            borderHighlight = GlassBorderHighlight,
            borderSubtle = GlassBorderSubtle,
            accentCyan = HdrCyan,
            accentGold = HdrGold,
            accentEmerald = HdrEmerald,
            accentCoral = HdrCoral,
            textPrimary = TextPrimaryDark,
            textSecondary = TextSecondaryDark
        )
    } else {
        LiquidGlassColors(
            isDark = false,
            backdropBase = LightFrostVoid,
            glassFill = GlassFillLight,
            glassFillStrong = Color(0xDCFFFFFF),
            borderHighlight = GlassBorderLightHighlight,
            borderSubtle = GlassBorderLightSubtle,
            accentCyan = HdrCyanDeep,
            accentGold = HdrGoldDeep,
            accentEmerald = Color(0xFF00945E),
            accentCoral = Color(0xFFCF2E43),
            textPrimary = TextPrimaryLight,
            textSecondary = TextSecondaryLight
        )
    }

    CompositionLocalProvider(LocalLiquidGlassColors provides glassColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = HdrStudioTypography,
            shapes = HdrStudioShapes,
            content = content
        )
    }
}
