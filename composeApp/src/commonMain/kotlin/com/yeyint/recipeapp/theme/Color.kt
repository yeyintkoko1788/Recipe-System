package com.yeyint.recipeapp.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette carried over from the ComposeBase design system (brand purple and
 * semantic colors), extended with dark-mode counterparts.
 */
object AppColors {
    // Brand
    val BrandPrimary = Color(0xFF864DF9)
    val BrandPrimaryDisabled = Color(0x66864DF9)
    val BrandSurfaceTint = Color(0xFFF8F5FE)

    // Neutrals (light)
    val NeutralText = Color(0xFF1C1C1C)
    val NeutralTextLight = Color(0xFF8D8D8D)
    val Surface = Color(0xFFF9F9F9)
    val CardBackground = Color(0xFFF6F7F9)
    val BorderLight = Color(0xFFDBDBDB)

    // Neutrals (dark)
    val DarkBackground = Color(0xFF121212)
    val DarkSurface = Color(0xFF1E1E1E)
    val DarkCard = Color(0xFF262626)
    val DarkText = Color(0xFFF2F2F2)
    val DarkTextSecondary = Color(0xFFB3B3B3)

    // Semantic
    val Error = Color(0xFFE6000F)
    val ErrorLight = Color(0xFFFB5E69)
    val Success = Color(0xFF04B435)
    val SuccessText = Color(0xFF16A34A)
    val Warning = Color(0xFFFB8C00)
    val Info = Color(0xFF2176FF)
}
