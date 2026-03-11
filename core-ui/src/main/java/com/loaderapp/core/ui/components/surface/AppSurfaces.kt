package com.loaderapp.core.ui.components.surface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.ShapeCard
import com.loaderapp.core.ui.theme.pressScale

@Composable
fun AppSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = AppSurfaceDefaults.contentPadding(),
    content: @Composable ColumnScope.() -> Unit,
) {
    AppSurfaceContainer(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        style = AppSurfaceStyle.Filled,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun AppOutlinedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = AppSurfaceDefaults.contentPadding(),
    content: @Composable ColumnScope.() -> Unit,
) {
    AppSurfaceContainer(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        style = AppSurfaceStyle.Outlined,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun AppElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = AppSurfaceDefaults.contentPadding(),
    content: @Composable ColumnScope.() -> Unit,
) {
    AppSurfaceContainer(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        style = AppSurfaceStyle.Elevated,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun AppListItemContainer(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = AppSurfaceDefaults.listItemPadding(),
    content: @Composable RowScope.() -> Unit,
) {
    AppSurfaceContainer(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        style = AppSurfaceStyle.ListItem,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
            content = content,
        )
    }
}

@Composable
private fun AppSurfaceContainer(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    style: AppSurfaceStyle,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colorScheme = AppSurfaceDefaults.colorScheme(style)
    val colors = cardColors(colorScheme = colorScheme)
    val elevation = cardElevation(style = style)

    when (style) {
        AppSurfaceStyle.Filled,
        AppSurfaceStyle.ListItem,
        ->
            AppFilledSurfaceCard(
                modifier = modifier,
                onClick = onClick,
                enabled = enabled,
                colors = colors,
                elevation = elevation,
                content = content,
            )

        AppSurfaceStyle.Outlined ->
            AppOutlinedSurfaceCard(
                modifier = modifier,
                onClick = onClick,
                enabled = enabled,
                border =
                    AppSurfaceDefaults.outlinedBorderStroke(
                        enabled = enabled,
                        color = colorScheme.borderColor,
                    ),
                colors = colors,
                elevation = elevation,
                content = content,
            )

        AppSurfaceStyle.Elevated ->
            AppElevatedSurfaceCard(
                modifier = modifier,
                onClick = onClick,
                enabled = enabled,
                colors = colors,
                elevation = elevation,
                content = content,
            )
    }
}

@Composable
private fun AppFilledSurfaceCard(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    colors: CardColors,
    elevation: CardElevation,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardModifier = modifier.cardPressScale(onClick = onClick, enabled = enabled)
    Card(
        modifier = cardModifier,
        shape = ShapeCard,
        colors = colors,
        elevation = elevation,
        content = content,
    )
}

@Composable
private fun AppOutlinedSurfaceCard(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    border: BorderStroke,
    colors: CardColors,
    elevation: CardElevation,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardModifier = modifier.cardPressScale(onClick = onClick, enabled = enabled)
    OutlinedCard(
        modifier = cardModifier,
        shape = ShapeCard,
        border = border,
        colors = colors,
        elevation = elevation,
        content = content,
    )
}

@Composable
private fun AppElevatedSurfaceCard(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    colors: CardColors,
    elevation: CardElevation,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardModifier = modifier.cardPressScale(onClick = onClick, enabled = enabled)
    ElevatedCard(
        modifier = cardModifier,
        shape = ShapeCard,
        colors = colors,
        elevation = elevation,
        content = content,
    )
}

@Composable
private fun Modifier.cardPressScale(
    onClick: (() -> Unit)?,
    enabled: Boolean,
): Modifier {
    if (onClick == null) return this

    val interactionSource = remember { MutableInteractionSource() }

    return this
        .pressScale(
            enabled = enabled,
            interactionSource = interactionSource,
        ).clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
}

@Composable
private fun cardColors(colorScheme: AppSurfaceColors): CardColors =
    CardDefaults.cardColors(
        containerColor = colorScheme.containerColor,
        contentColor = colorScheme.contentColor,
        disabledContainerColor = AppSurfaceDefaults.disabledContainerColor(colorScheme.containerColor),
        disabledContentColor = AppSurfaceDefaults.disabledContentColor(colorScheme.contentColor),
    )

@Composable
private fun cardElevation(style: AppSurfaceStyle): CardElevation =
    when (style) {
        AppSurfaceStyle.Elevated ->
            CardDefaults.elevatedCardElevation(
                defaultElevation = AppSurfaceDefaults.ElevatedDefaultElevation,
                pressedElevation = AppSurfaceDefaults.ElevatedPressedElevation,
                focusedElevation = AppSurfaceDefaults.ElevatedFocusedElevation,
                hoveredElevation = AppSurfaceDefaults.ElevatedHoveredElevation,
                draggedElevation = AppSurfaceDefaults.ElevatedDraggedElevation,
                disabledElevation = AppSurfaceDefaults.ElevatedDisabledElevation,
            )

        else ->
            CardDefaults.cardElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                draggedElevation = 0.dp,
                disabledElevation = 0.dp,
            )
    }
