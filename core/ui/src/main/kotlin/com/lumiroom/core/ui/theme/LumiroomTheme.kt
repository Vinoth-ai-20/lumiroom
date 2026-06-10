package com.lumiroom.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

// ── Dark Color Scheme (Primary) ───────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = LumiroomPrimary,
    onPrimary            = LumiroomOnPrimary,
    primaryContainer     = LumiroomPrimaryContainer,
    onPrimaryContainer   = LumiroomOnPrimaryContainer,
    secondary            = LumiroomSecondary,
    onSecondary          = LumiroomOnSecondary,
    secondaryContainer   = LumiroomSecondaryContainer,
    onSecondaryContainer = LumiroomOnSecondaryContainer,
    tertiary             = LumiroomTertiary,
    onTertiary           = LumiroomOnTertiary,
    tertiaryContainer    = LumiroomTertiaryContainer,
    onTertiaryContainer  = LumiroomOnTertiaryContainer,
    error                = LumiroomError,
    onError              = LumiroomOnError,
    errorContainer       = LumiroomErrorContainer,
    onErrorContainer     = LumiroomOnErrorContainer,
    background           = LumiroomBackground,
    onBackground         = LumiroomOnBackground,
    surface              = LumiroomSurface,
    onSurface            = LumiroomOnSurface,
    surfaceVariant       = LumiroomSurfaceVariant,
    onSurfaceVariant     = LumiroomOnSurfaceVariant,
    outline              = LumiroomOutline,
    outlineVariant       = LumiroomOutlineVariant,
)

// ── Light Color Scheme ────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary              = LumiroomPrimary,
    onPrimary            = LumiroomOnPrimary,
    primaryContainer     = LumiroomPrimaryContainer,
    onPrimaryContainer   = LumiroomOnPrimaryContainer,
    secondary            = LumiroomSecondary,
    onSecondary          = LumiroomOnSecondary,
    secondaryContainer   = LumiroomSecondaryContainer,
    onSecondaryContainer = LumiroomOnSecondaryContainer,
    background           = LumiroomBackgroundLight,
    surface              = LumiroomSurfaceLight,
    surfaceVariant       = LumiroomSurfaceVariantLight,
)

// ── Custom Color Tokens (AR overlay, glass) ───────────────────────────────────
data class LumiroomCustomColors(
    val glassLight: androidx.compose.ui.graphics.Color = LumiroomGlassLight,
    val glassMedium: androidx.compose.ui.graphics.Color = LumiroomGlassMedium,
    val glassBorder: androidx.compose.ui.graphics.Color = LumiroomGlassBorder,
    val arPlane: androidx.compose.ui.graphics.Color = ArPlaneColor,
    val arSelection: androidx.compose.ui.graphics.Color = ArSelectionRing,
    val arMeasurementBg: androidx.compose.ui.graphics.Color = ArMeasurementBg,
)

val LocalLumiroomColors = staticCompositionLocalOf { LumiroomCustomColors() }

// ── Theme Composable ──────────────────────────────────────────────────────────
@Composable
fun LumiroomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalLumiroomColors provides LumiroomCustomColors(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = LumiroomTypography,
            shapes      = LumiroomShapes,
            content     = content,
        )
    }
}

/** Convenience accessor for custom Lumiroom colors via [MaterialTheme]. */
val MaterialTheme.lumiroomColors: LumiroomCustomColors
    @Composable
    get() = LocalLumiroomColors.current
