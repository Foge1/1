package com.loaderapp.core.ui.components.listitem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.core.ui.theme.AppTypography

@Immutable
data class AppListItemColors(
    val headlineColor: Color,
    val supportingColor: Color,
)

@Immutable
data class AppListItemStyle(
    val minHeight: Dp,
    val horizontalContentSpacing: Dp,
    val textVerticalSpacing: Dp,
    val headlineTextStyle: TextStyle,
    val supportingTextStyle: TextStyle,
)

object AppListItemDefaults {
    fun colors(
        headlineColor: Color = AppColors.Foreground,
        supportingColor: Color = AppColors.MutedForeground,
    ): AppListItemColors =
        AppListItemColors(
            headlineColor = headlineColor,
            supportingColor = supportingColor,
        )

    fun style(
        minHeight: Dp = 56.dp,
        horizontalContentSpacing: Dp = AppSpacing.md,
        textVerticalSpacing: Dp = AppSpacing.xs,
        headlineTextStyle: TextStyle = AppTypography.bodyLarge,
        supportingTextStyle: TextStyle = AppTypography.bodyMedium,
    ): AppListItemStyle =
        AppListItemStyle(
            minHeight = minHeight,
            horizontalContentSpacing = horizontalContentSpacing,
            textVerticalSpacing = textVerticalSpacing,
            headlineTextStyle = headlineTextStyle,
            supportingTextStyle = supportingTextStyle,
        )
}
