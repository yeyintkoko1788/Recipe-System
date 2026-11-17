package com.yeyint.recipeapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val LightColors = lightColorScheme(
    primary = AppColors.BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = AppColors.BrandSurfaceTint,
    onPrimaryContainer = AppColors.BrandPrimary,
    secondary = AppColors.Info,
    background = AppColors.Surface,
    onBackground = AppColors.NeutralText,
    surface = AppColors.Surface,
    onSurface = AppColors.NeutralText,
    surfaceVariant = AppColors.CardBackground,
    onSurfaceVariant = AppColors.NeutralTextLight,
    outline = AppColors.BorderLight,
    error = AppColors.Error,
)

private val DarkColors = darkColorScheme(
    primary = AppColors.BrandPrimaryDark,
    onPrimary = AppColors.OnBrandPrimaryDark,
    primaryContainer = AppColors.DarkCard,
    onPrimaryContainer = AppColors.BrandPrimaryDark,
    secondary = AppColors.Info,
    background = AppColors.DarkBackground,
    onBackground = AppColors.DarkText,
    surface = AppColors.DarkSurface,
    onSurface = AppColors.DarkText,
    surfaceVariant = AppColors.DarkCard,
    onSurfaceVariant = AppColors.DarkTextSecondary,
    outline = AppColors.DarkTextSecondary,
    error = AppColors.ErrorLight,
)

/**
 * App theme. [darkModeOverride] comes from user settings:
 * null = follow system, true/false = forced.
 */
@Composable
fun RecipeAppTheme(
    darkModeOverride: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = darkModeOverride ?: isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
