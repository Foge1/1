package com.loaderapp.core.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.loaderapp.core.ui.components.button.AppPrimaryButton
import com.loaderapp.core.ui.components.button.AppSecondaryButton
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.core.ui.theme.AppTypography
import com.loaderapp.core.ui.theme.ShapeDialog

@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties =
            DialogProperties(
                dismissOnBackPress = dismissOnBackPress,
                dismissOnClickOutside = dismissOnClickOutside,
                usePlatformDefaultWidth = false,
            ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(AppSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .widthIn(max = AppDialogDefaults.MaxWidth),
                shape = ShapeDialog,
                color = AppDialogDefaults.containerColor(),
                contentColor = AppDialogDefaults.contentColor(),
            ) {
                Column(
                    modifier = Modifier.padding(AppDialogDefaults.contentPadding()),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun AppAlertDialog(
    title: String,
    text: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    AppDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppDialogDefaults.TitleBodySpacing),
        ) {
            Text(
                text = title,
                style = AppTypography.titleLarge,
            )
            Text(
                text = text,
                style = AppTypography.bodyMedium,
                color = AppDialogDefaults.supportingTextColor(),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = AppDialogDefaults.BodyActionsSpacing),
            horizontalArrangement =
                Arrangement.spacedBy(
                    space = AppDialogDefaults.ActionButtonsSpacing,
                    alignment = Alignment.End,
                ),
        ) {
            dismissButton?.invoke()
            confirmButton()
        }
    }
}

@Composable
fun AppConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
) {
    AppAlertDialog(
        title = title,
        text = text,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        confirmButton = {
            AppPrimaryButton(
                text = confirmText,
                onClick = onConfirm,
            )
        },
        dismissButton =
            dismissText?.let { textValue ->
                {
                    AppSecondaryButton(
                        text = textValue,
                        onClick = onDismissRequest,
                    )
                }
            },
    )
}
