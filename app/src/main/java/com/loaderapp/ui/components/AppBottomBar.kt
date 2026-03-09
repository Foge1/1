package com.loaderapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppMotion
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.core.ui.theme.AppTypography
import com.loaderapp.core.ui.theme.ShapeBottomBar
import com.loaderapp.ui.theme.LoaderAppTheme

private const val MAX_BADGE_VALUE = 99
private const val ACTIVE_CONTENT_ALPHA = 1f
private const val INACTIVE_CONTENT_ALPHA = 0.64f
private const val ACTIVE_CAPSULE_ALPHA = 0.42f
private const val INACTIVE_INDICATOR_ALPHA = 0f
private const val SELECTED_SCALE = 1.08f
private const val UNSELECTED_SCALE = 1f

/**
 * Элемент нижней навигации.
 */
data class BottomNavItem(
    val icon: ImageVector,
    val label: String,
    val badgeCount: Int = 0,
)

@Composable
fun AppBottomBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeBottomBar,
        color = AppColors.Surface,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            items.forEachIndexed { index, item ->
                BottomNavItemView(
                    item = item,
                    isSelected = index == selectedIndex,
                    onClick = { onItemSelected(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val itemVisuals = rememberBottomNavItemVisuals(isSelected = isSelected)

    Column(
        modifier =
            modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(vertical = AppSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        TopSelectionIndicator(indicatorAlpha = itemVisuals.indicatorAlpha)
        Spacer(Modifier.height(AppSpacing.xs))
        IconCapsule(item = item, visuals = itemVisuals)
        Spacer(Modifier.height(AppSpacing.xs))
        Text(
            text = item.label,
            style =
                AppTypography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                ),
            color = itemVisuals.contentColor,
            maxLines = 1,
        )
    }
}

private data class BottomNavItemVisuals(
    val scale: Float,
    val contentColor: Color,
    val capsuleAlpha: Float,
    val indicatorAlpha: Float,
)

@Composable
private fun rememberBottomNavItemVisuals(isSelected: Boolean): BottomNavItemVisuals {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) SELECTED_SCALE else UNSELECTED_SCALE,
        animationSpec = AppMotion.SPRING_MEDIUM_BOUNCE,
        label = "bottomNavScale",
    )
    val contentColor by animateColorAsState(
        targetValue =
            if (isSelected) {
                AppColors.Primary.copy(alpha = ACTIVE_CONTENT_ALPHA)
            } else {
                AppColors.MutedForeground.copy(alpha = INACTIVE_CONTENT_ALPHA)
            },
        animationSpec =
            androidx.compose.animation.core.tween(
                durationMillis = AppMotion.DURATION_MEDIUM,
                easing = AppMotion.EASING_STANDARD,
            ),
        label = "bottomNavContentColor",
    )
    val capsuleAlpha by animateFloatAsState(
        targetValue = if (isSelected) ACTIVE_CAPSULE_ALPHA else 0f,
        animationSpec = AppMotion.tweenMedium(),
        label = "bottomNavCapsuleAlpha",
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isSelected) ACTIVE_CONTENT_ALPHA else INACTIVE_INDICATOR_ALPHA,
        animationSpec = AppMotion.tweenMedium(),
        label = "bottomNavIndicatorAlpha",
    )

    return BottomNavItemVisuals(
        scale = scale,
        contentColor = contentColor,
        capsuleAlpha = capsuleAlpha,
        indicatorAlpha = indicatorAlpha,
    )
}

@Composable
private fun TopSelectionIndicator(indicatorAlpha: Float) {
    Box(
        modifier =
            Modifier
                .width(24.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(AppColors.Primary.copy(alpha = indicatorAlpha)),
    )
}

@Composable
private fun IconCapsule(
    item: BottomNavItem,
    visuals: BottomNavItemVisuals,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .scale(visuals.scale)
                .clip(RoundedCornerShape(percent = 50))
                .background(AppColors.PrimaryContainer.copy(alpha = visuals.capsuleAlpha))
                .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        if (item.badgeCount > 0) {
            BadgedIcon(item = item, color = visuals.contentColor)
        } else {
            BaseIcon(item = item, color = visuals.contentColor)
        }
    }
}

@Composable
private fun BadgedIcon(
    item: BottomNavItem,
    color: Color,
) {
    BadgedBox(
        badge = {
            Badge(
                containerColor = AppColors.Accent,
                contentColor = AppColors.OnPrimary,
            ) {
                val badgeText =
                    if (item.badgeCount > MAX_BADGE_VALUE) {
                        "$MAX_BADGE_VALUE+"
                    } else {
                        item.badgeCount.toString()
                    }
                Text(
                    text = badgeText,
                    style = AppTypography.labelSmall,
                )
            }
        },
    ) {
        BaseIcon(item = item, color = color)
    }
}

@Composable
private fun BaseIcon(
    item: BottomNavItem,
    color: Color,
) {
    Icon(
        imageVector = item.icon,
        contentDescription = item.label,
        tint = color,
        modifier = Modifier.size(22.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun AppBottomBarPreview() {
    LoaderAppTheme {
        AppBottomBar(
            items =
                listOf(
                    BottomNavItem(Icons.Default.Home, "Заказы"),
                    BottomNavItem(Icons.Default.History, "Отклики", badgeCount = 3),
                    BottomNavItem(Icons.Default.Star, "Рейтинг"),
                    BottomNavItem(Icons.Default.Person, "Профиль"),
                    BottomNavItem(Icons.Default.Settings, "Настройки"),
                ),
            selectedIndex = 0,
            onItemSelected = {},
        )
    }
}
