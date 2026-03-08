package com.loaderapp.core.ui.components.selection

import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppShapes
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.core.ui.theme.AppTypography

@Immutable
data class AppSelectableRowStyle(
    val shape: Shape,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val itemSpacing: Dp,
    val minHeight: Dp,
)

object AppSelectionDefaults {
    private const val DisabledAlpha = 0.38f

    val borderWidth: Dp = 1.dp

    @Composable
    fun checkboxColors(): CheckboxColors =
        CheckboxDefaults.colors(
            checkedColor = AppColors.Primary,
            uncheckedColor = AppColors.Border,
            checkmarkColor = AppColors.Surface,
            disabledCheckedColor = AppColors.Primary.copy(alpha = DisabledAlpha),
            disabledUncheckedColor = AppColors.Border.copy(alpha = DisabledAlpha),
            disabledIndeterminateColor = AppColors.Primary.copy(alpha = DisabledAlpha),
        )

    @Composable
    fun radioButtonColors(): RadioButtonColors =
        RadioButtonDefaults.colors(
            selectedColor = AppColors.Primary,
            unselectedColor = AppColors.Border,
            disabledSelectedColor = AppColors.Primary.copy(alpha = DisabledAlpha),
            disabledUnselectedColor = AppColors.Border.copy(alpha = DisabledAlpha),
        )

    @Composable
    fun switchColors(): SwitchColors =
        SwitchDefaults.colors(
            checkedThumbColor = AppColors.Primary,
            checkedTrackColor = AppColors.Primary.copy(alpha = 0.28f),
            checkedBorderColor = AppColors.Primary,
            checkedIconColor = AppColors.Surface,
            uncheckedThumbColor = AppColors.Surface,
            uncheckedTrackColor = AppColors.Surface,
            uncheckedBorderColor = AppColors.Border,
            uncheckedIconColor = AppColors.OnPrimary,
            disabledCheckedThumbColor = AppColors.Primary.copy(alpha = DisabledAlpha),
            disabledCheckedTrackColor = AppColors.Primary.copy(alpha = 0.16f),
            disabledCheckedBorderColor = AppColors.Primary.copy(alpha = DisabledAlpha),
            disabledCheckedIconColor = AppColors.Surface.copy(alpha = DisabledAlpha),
            disabledUncheckedThumbColor = AppColors.Surface.copy(alpha = DisabledAlpha),
            disabledUncheckedTrackColor = AppColors.Surface.copy(alpha = 0.5f),
            disabledUncheckedBorderColor = AppColors.Border.copy(alpha = DisabledAlpha),
            disabledUncheckedIconColor = AppColors.OnPrimary.copy(alpha = DisabledAlpha),
        )

    fun selectableRowStyle(
        shape: Shape = AppShapes.medium,
        horizontalPadding: Dp = AppSpacing.lg,
        verticalPadding: Dp = AppSpacing.md,
        itemSpacing: Dp = AppSpacing.md,
        minHeight: Dp = 56.dp,
    ): AppSelectableRowStyle =
        AppSelectableRowStyle(
            shape = shape,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
            itemSpacing = itemSpacing,
            minHeight = minHeight,
        )

    val selectableRowTextStyle = AppTypography.bodyLarge

    fun selectableRowContainerColor(selected: Boolean): Color =
        if (selected) {
            AppColors.Primary.copy(alpha = 0.12f)
        } else {
            AppColors.Surface
        }

    fun selectableRowContentColor(enabled: Boolean): Color =
        if (enabled) {
            AppColors.Foreground
        } else {
            AppColors.Foreground.copy(alpha = DisabledAlpha)
        }

    fun selectableRowBorderColor(selected: Boolean, enabled: Boolean): Color {
        val baseColor =
            if (selected) {
                AppColors.Primary
            } else {
                AppColors.Border
            }

        return if (enabled) {
            baseColor
        } else {
            baseColor.copy(alpha = DisabledAlpha)
        }
    }
}
