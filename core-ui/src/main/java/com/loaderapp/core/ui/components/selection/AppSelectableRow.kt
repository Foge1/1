package com.loaderapp.core.ui.components.selection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContentColor
import androidx.compose.ui.semantics.Role

@Composable
fun AppSelectableRow(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val style = AppSelectionDefaults.selectableRowStyle()
    val containerColor = AppSelectionDefaults.selectableRowContainerColor(selected = selected)
    val contentColor = AppSelectionDefaults.selectableRowContentColor(enabled = enabled)
    val borderColor =
        AppSelectionDefaults.selectableRowBorderColor(
            selected = selected,
            enabled = enabled,
        )

    Surface(
        modifier =
            modifier.selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = style.shape,
        color = containerColor,
        contentColor = contentColor,
        border =
            BorderStroke(
                width = AppSelectionDefaults.BORDER_WIDTH,
                color = borderColor,
            ),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides AppSelectionDefaults.selectableRowTextStyle,
        ) {
            Row(
                modifier =
                    Modifier
                        .defaultMinSize(minHeight = style.minHeight)
                        .padding(
                            horizontal = style.horizontalPadding,
                            vertical = style.verticalPadding,
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(style.itemSpacing),
            ) {
                leading?.invoke()
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    content()
                }
                trailing?.invoke()
            }
        }
    }
}
