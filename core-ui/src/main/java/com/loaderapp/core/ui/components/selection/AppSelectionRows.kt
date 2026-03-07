package com.loaderapp.core.ui.components.selection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.loaderapp.core.ui.components.listitem.AppClickableListItem
import com.loaderapp.core.ui.components.listitem.AppListItemDefaults

@Composable
fun AppCheckboxRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    style: AppSelectionRowStyle = AppSelectionRowDefaults.style(),
) {
    AppSelectionRow(
        title = title,
        modifier = modifier.semantics { role = Role.Checkbox },
        enabled = enabled,
        supportingText = supportingText,
        onClick = { onCheckedChange(!checked) },
        style = style,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

@Composable
fun AppSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    style: AppSelectionRowStyle = AppSelectionRowDefaults.style(),
) {
    AppSelectionRow(
        title = title,
        modifier = modifier.semantics { role = Role.Switch },
        enabled = enabled,
        supportingText = supportingText,
        onClick = { onCheckedChange(!checked) },
        style = style,
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
        )
    }
}

@Composable
fun AppRadioRow(
    title: String,
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    style: AppSelectionRowStyle = AppSelectionRowDefaults.style(),
) {
    AppSelectionRow(
        title = title,
        modifier = modifier.semantics { role = Role.RadioButton },
        enabled = enabled,
        supportingText = supportingText,
        onClick = onSelected,
        style = style,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
        )
    }
}

@Composable
private fun AppSelectionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    style: AppSelectionRowStyle = AppSelectionRowDefaults.style(),
    trailingControl: @Composable () -> Unit,
) {
    AppClickableListItem(
        headlineText = title,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        supportingText = supportingText,
        trailingContent = {
            Box(
                modifier = Modifier.widthIn(min = style.trailingControlMinSize),
                contentAlignment = Alignment.CenterEnd,
            ) {
                trailingControl()
            }
        },
        style =
            AppListItemDefaults.style(
                minHeight = style.minHeight,
                horizontalContentSpacing = style.horizontalContentSpacing,
                textVerticalSpacing = style.textVerticalSpacing,
            ),
    )
}
