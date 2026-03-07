package com.loaderapp.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

val CoreUiDarkColorScheme =
    darkColorScheme(
        primary = AppColors.Primary,
        onPrimary = AppColors.OnPrimary,
        primaryContainer = AppColors.PrimaryContainer,
        background = AppColors.Background,
        surface = AppColors.Surface,
        surfaceVariant = AppColors.Muted,
        onSurfaceVariant = AppColors.MutedForeground,
        onSurface = AppColors.Foreground,
        outline = AppColors.Border,
        error = AppColors.Destructive,
    )

@Composable
fun CoreUiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CoreUiDarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
