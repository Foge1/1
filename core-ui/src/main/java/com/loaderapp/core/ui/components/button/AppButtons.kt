package com.loaderapp.core.ui.components.button

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.ShapeButton

@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    AppTextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        style = AppButtonStyle.Primary,
    )
}

@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    AppTextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        style = AppButtonStyle.Secondary,
    )
}

@Composable
fun AppTertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    AppTextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        style = AppButtonStyle.Tertiary,
    )
}

@Composable
fun AppDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    AppTextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        loading = loading,
        style = AppButtonStyle.Danger,
    )
}

@Composable
private fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    loading: Boolean,
    style: AppButtonStyle,
) {
    val colorScheme = AppButtonDefaults.colorScheme(style)
    val effectiveEnabled = enabled && !loading
    val colors = buttonColors(style = style, colorScheme = colorScheme)
    val contentModifier = Modifier.defaultMinSize(minHeight = AppButtonDefaults.MinHeight)

    when (style) {
        AppButtonStyle.Primary,
        AppButtonStyle.Danger,
        -> {
            Button(
                onClick = onClick,
                enabled = effectiveEnabled,
                modifier = modifier.then(contentModifier),
                shape = ShapeButton,
                colors = colors,
                contentPadding = AppButtonDefaults.contentPadding(),
            ) {
                AppButtonLabel(
                    text = text,
                    loading = loading,
                    contentColor = colorScheme.contentColor,
                )
            }
        }

        AppButtonStyle.Secondary ->
            OutlinedButton(
                onClick = onClick,
                enabled = effectiveEnabled,
                modifier = modifier.then(contentModifier),
                shape = ShapeButton,
                colors = colors,
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            AppButtonDefaults.borderColor(
                                color = colorScheme.outlinedBorderColor,
                                enabled = effectiveEnabled,
                            ),
                    ),
                contentPadding = AppButtonDefaults.contentPadding(),
            ) {
                AppButtonLabel(
                    text = text,
                    loading = loading,
                    contentColor = colorScheme.contentColor,
                )
            }

        AppButtonStyle.Tertiary ->
            TextButton(
                onClick = onClick,
                enabled = effectiveEnabled,
                modifier = modifier.then(contentModifier),
                shape = ShapeButton,
                colors = colors,
                contentPadding = AppButtonDefaults.contentPadding(),
            ) {
                AppButtonLabel(
                    text = text,
                    loading = loading,
                    contentColor = colorScheme.contentColor,
                )
            }
    }
}

@Composable
private fun AppButtonLabel(
    text: String,
    loading: Boolean,
    contentColor: Color,
) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.alpha(if (loading) 0f else 1f),
        )
        AnimatedVisibility(visible = loading) {
            CircularProgressIndicator(
                color = contentColor,
                strokeWidth = 2.dp,
                modifier = Modifier.size(AppButtonDefaults.IconSize),
            )
        }
    }
}

@Composable
private fun buttonColors(
    style: AppButtonStyle,
    colorScheme: AppButtonColorScheme,
): ButtonColors {
    val disabledContainerColor =
        AppButtonDefaults.disabledContainerColor(colorScheme.containerColor)
    val disabledContentColor =
        AppButtonDefaults.disabledContentColor(colorScheme.contentColor)

    return when (style) {
        AppButtonStyle.Secondary ->
            ButtonDefaults.outlinedButtonColors(
                containerColor = colorScheme.containerColor,
                contentColor = colorScheme.contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
            )

        AppButtonStyle.Tertiary ->
            ButtonDefaults.textButtonColors(
                containerColor = colorScheme.containerColor,
                contentColor = colorScheme.contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
            )

        else ->
            ButtonDefaults.buttonColors(
                containerColor = colorScheme.containerColor,
                contentColor = colorScheme.contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor,
            )
    }
}
