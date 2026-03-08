package com.loaderapp.core.ui.components.dialog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppSpacing

@Immutable
object AppDialogDefaults {
    val MaxWidth: Dp = 560.dp

    val TitleBodySpacing: Dp = AppSpacing.md
    val BodyActionsSpacing: Dp = AppSpacing.xxl
    val ActionButtonsSpacing: Dp = AppSpacing.sm

    fun contentPadding(): PaddingValues =
        PaddingValues(
            horizontal = AppSpacing.xxl,
            vertical = AppSpacing.xxl,
        )

    @Composable
    fun containerColor(): Color = AppColors.Surface

    @Composable
    fun contentColor(): Color = AppColors.Foreground

    @Composable
    fun supportingTextColor(): Color = AppColors.MutedForeground
}
