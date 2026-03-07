package com.loaderapp.core.ui.components.selection

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppSpacing

@Immutable
data class AppSelectionRowStyle(
    val minHeight: Dp,
    val horizontalContentSpacing: Dp,
    val textVerticalSpacing: Dp,
    val trailingControlMinSize: Dp,
)

object AppSelectionRowDefaults {
    fun style(
        minHeight: Dp = 56.dp,
        horizontalContentSpacing: Dp = AppSpacing.md,
        textVerticalSpacing: Dp = AppSpacing.xs,
        trailingControlMinSize: Dp = 40.dp,
    ): AppSelectionRowStyle =
        AppSelectionRowStyle(
            minHeight = minHeight,
            horizontalContentSpacing = horizontalContentSpacing,
            textVerticalSpacing = textVerticalSpacing,
            trailingControlMinSize = trailingControlMinSize,
        )
}
