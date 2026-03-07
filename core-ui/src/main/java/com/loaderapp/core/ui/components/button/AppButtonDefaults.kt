package com.loaderapp.core.ui.components.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppColors

internal enum class AppButtonStyle {
    Primary,
    Secondary,
    Tertiary,
    Danger,
}

@Immutable
internal data class AppButtonColorScheme(
    val containerColor: Color,
    val contentColor: Color,
    val outlinedBorderColor: Color = Color.Transparent,
)

internal object AppButtonDefaults {
    val MinHeight: Dp = 48.dp
    val HorizontalPadding: Dp = 20.dp
    val VerticalPadding: Dp = 12.dp
    val IconButtonSize: Dp = 48.dp
    val IconSize: Dp = 20.dp

    private const val DisabledContainerOpacity: Float = 0.38f
    private const val DisabledContentOpacity: Float = 0.6f

    fun colorScheme(style: AppButtonStyle): AppButtonColorScheme =
        when (style) {
            AppButtonStyle.Primary ->
                AppButtonColorScheme(
                    containerColor = AppColors.Primary,
                    contentColor = AppColors.OnPrimary,
                )

            AppButtonStyle.Secondary ->
                AppButtonColorScheme(
                    containerColor = Color.Transparent,
                    contentColor = AppColors.Primary,
                    outlinedBorderColor = AppColors.Border,
                )

            AppButtonStyle.Tertiary ->
                AppButtonColorScheme(
                    containerColor = Color.Transparent,
                    contentColor = AppColors.Foreground,
                )

            AppButtonStyle.Danger ->
                AppButtonColorScheme(
                    containerColor = AppColors.Destructive,
                    contentColor = AppColors.Background,
                )
        }

    fun disabledContainerColor(color: Color): Color = color.copy(alpha = DisabledContainerOpacity)

    fun disabledContentColor(color: Color): Color = color.copy(alpha = DisabledContentOpacity)

    fun borderColor(
        color: Color,
        enabled: Boolean,
    ): Color =
        if (enabled) {
            color
        } else {
            disabledContainerColor(color)
        }

    @Composable
    fun contentPadding(): PaddingValues =
        ButtonDefaults.ContentPadding.copy(
            start = HorizontalPadding,
            top = VerticalPadding,
            end = HorizontalPadding,
            bottom = VerticalPadding,
        )
}
