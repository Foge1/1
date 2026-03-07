package com.loaderapp.core.ui.components.input

import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppShapes
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.core.ui.theme.AppTypography

@Immutable
data class AppTextFieldStyle(
    val shape: Shape,
    val minHeight: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
)

object AppTextFieldDefaults {
    fun style(
        shape: Shape = AppShapes.small,
        minHeight: Dp = 56.dp,
        horizontalPadding: Dp = AppSpacing.lg,
        verticalPadding: Dp = AppSpacing.md,
    ): AppTextFieldStyle =
        AppTextFieldStyle(
            shape = shape,
            minHeight = minHeight,
            horizontalPadding = horizontalPadding,
            verticalPadding = verticalPadding,
        )

    @Composable
    fun textFieldColors(): TextFieldColors = defaultTextFieldColors()

    @Composable
    fun outlinedTextFieldColors(): TextFieldColors = defaultTextFieldColors()

    val textStyle = AppTypography.bodyLarge
    val placeholderTextStyle = AppTypography.bodyMedium

    private fun defaultTextFieldColors(): TextFieldColors =
        TextFieldDefaults.colors(
            focusedContainerColor = AppColors.Surface,
            unfocusedContainerColor = AppColors.Surface,
            disabledContainerColor = AppColors.Muted,
            focusedTextColor = AppColors.Foreground,
            unfocusedTextColor = AppColors.Foreground,
            disabledTextColor = AppColors.MutedForeground,
            focusedPlaceholderColor = AppColors.MutedForeground,
            unfocusedPlaceholderColor = AppColors.MutedForeground,
            disabledPlaceholderColor = AppColors.MutedForeground,
            focusedLabelColor = AppColors.MutedForeground,
            unfocusedLabelColor = AppColors.MutedForeground,
            disabledLabelColor = AppColors.MutedForeground,
            focusedIndicatorColor = AppColors.Primary,
            unfocusedIndicatorColor = AppColors.Border,
            disabledIndicatorColor = AppColors.Border,
            focusedLeadingIconColor = AppColors.MutedForeground,
            unfocusedLeadingIconColor = AppColors.MutedForeground,
            disabledLeadingIconColor = AppColors.MutedForeground,
        )
}
