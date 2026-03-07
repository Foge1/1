package com.loaderapp.core.ui.components.listitem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.loaderapp.core.ui.components.surface.AppListItemContainer

@Composable
fun AppListItem(
    headlineText: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    colors: AppListItemColors = AppListItemDefaults.colors(),
    style: AppListItemStyle = AppListItemDefaults.style(),
) {
    AppBaseListItem(
        headlineText = headlineText,
        supportingText = supportingText,
        modifier = modifier,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        onClick = null,
        enabled = true,
        colors = colors,
        style = style,
    )
}

@Composable
fun AppClickableListItem(
    headlineText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingText: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    colors: AppListItemColors = AppListItemDefaults.colors(),
    style: AppListItemStyle = AppListItemDefaults.style(),
) {
    AppBaseListItem(
        headlineText = headlineText,
        supportingText = supportingText,
        modifier = modifier,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        style = style,
    )
}

@Composable
fun AppListItemHeader(
    text: String,
    modifier: Modifier = Modifier,
    colors: AppListItemColors = AppListItemDefaults.colors(),
    style: AppListItemStyle = AppListItemDefaults.style(),
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.headlineTextStyle,
        color = colors.headlineColor,
    )
}

@Composable
fun AppListItemSupportingText(
    text: String,
    modifier: Modifier = Modifier,
    colors: AppListItemColors = AppListItemDefaults.colors(),
    style: AppListItemStyle = AppListItemDefaults.style(),
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.supportingTextStyle,
        color = colors.supportingColor,
    )
}

@Composable
fun AppListItemTrailingContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd,
    ) {
        content()
    }
}

@Composable
private fun AppBaseListItem(
    headlineText: String,
    supportingText: String?,
    modifier: Modifier,
    leadingContent: (@Composable () -> Unit)?,
    trailingContent: (@Composable () -> Unit)?,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    colors: AppListItemColors,
    style: AppListItemStyle,
) {
    AppListItemContainer(
        modifier = modifier.defaultMinSize(minHeight = style.minHeight),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(style.horizontalContentSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingContent?.let { content ->
                Box(contentAlignment = Alignment.Center) {
                    content()
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(style.textVerticalSpacing),
            ) {
                AppListItemHeader(
                    text = headlineText,
                    colors = colors,
                    style = style,
                )

                supportingText?.let { text ->
                    AppListItemSupportingText(
                        text = text,
                        colors = colors,
                        style = style,
                    )
                }
            }

            trailingContent?.let { content ->
                AppListItemTrailingContent {
                    content()
                }
            }
        }
    }
}
