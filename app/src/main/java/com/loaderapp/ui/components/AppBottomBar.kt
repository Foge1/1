package com.loaderapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Модель вкладки ────────────────────────────────────────────────────────────

data class BottomNavItem(
    val icon: ImageVector,
    val label: String,
    val badgeCount: Int = 0
)

// ── Панель ────────────────────────────────────────────────────────────────────

@Composable
fun AppBottomBar(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier      = modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color         = MaterialTheme.colorScheme.surface,
        tonalElevation  = 0.dp,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            items.forEachIndexed { index, item ->
                BottomNavItemView(
                    item       = item,
                    isSelected = index == selectedIndex,
                    onClick    = { onItemSelected(index) },
                    modifier   = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Один элемент ──────────────────────────────────────────────────────────────

@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Масштаб капсулы — пружинная анимация при нажатии
    val scale by animateFloatAsState(
        targetValue    = if (isSelected) 1.08f else 1f,
        animationSpec  = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    // Цвет иконки и подписи
    val contentColor by animateColorAsState(
        targetValue   = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label         = "contentColor"
    )

    // Прозрачность фона капсулы
    val capsuleAlpha by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label         = "capsuleAlpha"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Линия-индикатор сверху (точно как на скриншоте)
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.Transparent
                )
        )

        Spacer(Modifier.height(4.dp))

        // Капсула с иконкой
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .scale(scale)
                .clip(RoundedCornerShape(50))
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(
                        alpha = capsuleAlpha * 0.6f
                    )
                )
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            // Badge
            if (item.badgeCount > 0) {
                BadgedBox(
                    badge = {
                        Badge {
                            Text(
                                text = if (item.badgeCount > 99) "99+" else "${item.badgeCount}",
                                fontSize = 8.sp
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector    = item.icon,
                        contentDescription = item.label,
                        tint           = contentColor,
                        modifier       = Modifier.size(22.dp)
                    )
                }
            } else {
                Icon(
                    imageVector    = item.icon,
                    contentDescription = item.label,
                    tint           = contentColor,
                    modifier       = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(3.dp))

        // Подпись
        Text(
            text       = item.label,
            fontSize   = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color      = contentColor,
            maxLines   = 1
        )
    }
}
