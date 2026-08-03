package com.yeyint.recipeapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = AppColors.BrandPrimary,
    onPrimary = AppColors.DarkText,
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
    primary = AppColors.BrandPrimary,
    onPrimary = AppColors.DarkText,
    primaryContainer = AppColors.DarkCard,
    onPrimaryContainer = AppColors.BrandPrimary,
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
        content = content,
    )
}
