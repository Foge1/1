package com.loaderapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Элемент нижней навигации.
 */
data class BottomNavItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    val shadowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        brush =
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, shadowColor.copy(alpha = 0.55f)),
                                startY = -24.dp.toPx(),
                                endY = 0f,
                            ),
                    )
                },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
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
        modifier = modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        TopSelectionIndicator(isSelected = isSelected)
        Spacer(Modifier.height(4.dp))
        IconCapsule(item = item, visuals = itemVisuals)
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = itemVisuals.contentColor,
            maxLines = 1,
        )
    }
}

private data class BottomNavItemVisuals(
    val scale: Float,
    val contentColor: Color,
    val capsuleAlpha: Float,
)

@Composable
private fun rememberBottomNavItemVisuals(isSelected: Boolean): BottomNavItemVisuals {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale",
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "contentColor",
    )
    val capsuleAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "capsuleAlpha",
    )
    return BottomNavItemVisuals(scale = scale, contentColor = contentColor, capsuleAlpha = capsuleAlpha)
}

@Composable
private fun TopSelectionIndicator(isSelected: Boolean) {
    Box(
        modifier =
            Modifier
                .width(24.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
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
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = visuals.capsuleAlpha * 0.6f))
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
            Badge {
                Text(text = if (item.badgeCount > 99) "99+" else "${item.badgeCount}", fontSize = 8.sp)
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
