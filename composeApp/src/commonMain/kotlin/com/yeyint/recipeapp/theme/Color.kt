package com.yeyint.recipeapp.theme

import androidx.compose.ui.graphics.Color

/**
 * Palette carried over from the ComposeBase design system (brand purple and
 * semantic colors), extended with dark-mode counterparts.
 */
object AppColors {
    // Brand
    val BrandPrimary = Color(0xFF0B7A43)
    val BrandPrimaryDisabled = Color(0x660B7A43)
    val BrandSurfaceTint = Color(0xFFE7F4EC)
    /** Lighter green that keeps contrast on dark surfaces. */
    val BrandPrimaryDark = Color(0xFF4CC38A)
    val OnBrandPrimaryDark = Color(0xFF00391F)

    // Neutrals (light)
    val NeutralText = Color(0xFF111418)
    val NeutralTextLight = Color(0xFF5B6470)
    val Surface = Color(0xFFFFFFFF)
    val CardBackground = Color(0xFFF3F4F6)
    val BorderLight = Color(0xFFE5E7EB)
    /** Floating bottom navigation pill. */
    val NavPill = Color(0xFF111418)
    val NavPillIcon = Color(0xFFC9CED6)

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
