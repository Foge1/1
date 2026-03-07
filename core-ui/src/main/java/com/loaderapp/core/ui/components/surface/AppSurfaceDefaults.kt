package com.loaderapp.core.ui.components.surface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppSpacing

internal enum class AppSurfaceStyle {
    Filled,
    Outlined,
    Elevated,
    ListItem,
}

@Immutable
internal data class AppSurfaceColors(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color = Color.Transparent,
)

object AppSurfaceDefaults {
    private const val DISABLED_CONTAINER_OPACITY: Float = 0.6f
    private const val DISABLED_CONTENT_OPACITY: Float = 0.6f
    private const val DISABLED_BORDER_OPACITY: Float = 0.4f

    internal val ElevatedDefaultElevation: Dp = 1.dp
    internal val ElevatedPressedElevation: Dp = 3.dp
    internal val ElevatedFocusedElevation: Dp = 1.dp
    internal val ElevatedHoveredElevation: Dp = 1.dp
    internal val ElevatedDraggedElevation: Dp = 2.dp
    internal val ElevatedDisabledElevation: Dp = 0.dp

    fun contentPadding(): PaddingValues =
        PaddingValues(
            horizontal = AppSpacing.lg,
            vertical = AppSpacing.lg,
        )

    fun compactContentPadding(): PaddingValues =
        PaddingValues(
            horizontal = AppSpacing.md,
            vertical = AppSpacing.md,
        )

    fun listItemPadding(): PaddingValues =
        PaddingValues(
            horizontal = AppSpacing.lg,
            vertical = AppSpacing.md,
        )

    fun outlinedBorderStroke(
        enabled: Boolean,
        color: Color = AppColors.Border,
    ): BorderStroke =
        BorderStroke(
            width = 1.dp,
            color = borderColor(color = color, enabled = enabled),
        )

    internal fun colorScheme(style: AppSurfaceStyle): AppSurfaceColors =
        when (style) {
            AppSurfaceStyle.Filled,
            AppSurfaceStyle.Elevated,
            AppSurfaceStyle.ListItem,
            ->
                AppSurfaceColors(
                    containerColor = AppColors.Surface,
                    contentColor = AppColors.Foreground,
                )

            AppSurfaceStyle.Outlined ->
                AppSurfaceColors(
                    containerColor = AppColors.Surface,
                    contentColor = AppColors.Foreground,
                    borderColor = AppColors.Border,
                )
        }

    internal fun disabledContainerColor(color: Color): Color = color.copy(alpha = DISABLED_CONTAINER_OPACITY)

    internal fun disabledContentColor(color: Color): Color = color.copy(alpha = DISABLED_CONTENT_OPACITY)

    internal fun borderColor(
        color: Color,
        enabled: Boolean,
    ): Color =
        if (enabled) {
            color
        } else {
            color.copy(alpha = DISABLED_BORDER_OPACITY)
        }
}
