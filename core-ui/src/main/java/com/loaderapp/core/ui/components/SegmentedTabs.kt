package com.loaderapp.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppMotion
import com.loaderapp.core.ui.theme.AppShapes
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.core.ui.theme.AppTypography
import com.loaderapp.core.ui.theme.CoreUiTheme

@Composable
fun SegmentedTabs(
    items: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .background(AppColors.Muted)
                .selectableGroup()
                .padding(AppSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val backgroundColor by
                animateColorAsState(
                    targetValue = if (isSelected) AppColors.Surface else Color.Transparent,
                    animationSpec =
                        tween(
                            durationMillis = AppMotion.DURATION_MEDIUM,
                            easing = AppMotion.EASING_STANDARD,
                        ),
                    label = "SegmentedTabsBackground",
                )
            val textColor by
                animateColorAsState(
                    targetValue =
                        if (isSelected) {
                            AppColors.Foreground
                        } else {
                            AppColors.MutedForeground
                        },
                    animationSpec =
                        tween(
                            durationMillis = AppMotion.DURATION_MEDIUM,
                            easing = AppMotion.EASING_STANDARD,
                        ),
                    label = "SegmentedTabsText",
                )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(AppShapes.small)
                    .background(backgroundColor)
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelectedChange(index) },
                        role = Role.Tab,
                    ).padding(
                        horizontal = AppSpacing.md,
                        vertical = AppSpacing.sm,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item,
                    style = AppTypography.labelLarge,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SegmentedTabsPreview() {
    CoreUiTheme {
        var selectedIndex by remember { mutableIntStateOf(0) }

        SegmentedTabs(
            items = listOf("Open", "In Progress", "Done"),
            selectedIndex = selectedIndex,
            onSelectedChange = { selectedIndex = it },
            modifier = Modifier.padding(AppSpacing.lg),
        )
    }
}
