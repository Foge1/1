package com.loaderapp.core.ui.components.bottomsheet

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppShapes
import com.loaderapp.core.ui.theme.AppSpacing

@Immutable
object AppBottomSheetDefaults {
    val sheetMaxWidth: Dp = 560.dp
    val headerSpacing: Dp = AppSpacing.md
    val contentSpacing: Dp = AppSpacing.lg

    fun sheetPadding(): PaddingValues =
        PaddingValues(
            horizontal = AppSpacing.xxl,
            vertical = AppSpacing.xxl,
        )

    val shape: Shape = AppShapes.extraLarge

    @Composable
    fun containerColor(): Color = AppColors.Surface

    @Composable
    fun contentColor(): Color = AppColors.Foreground
}
