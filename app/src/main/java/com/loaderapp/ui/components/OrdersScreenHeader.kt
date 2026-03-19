package com.loaderapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.core.ui.theme.AppTypography

private object OrdersScreenHeaderDefaults {
    val HorizontalPadding = AppSpacing.lg
    val HeaderTopPadding = AppSpacing.sm
    val RoleContextBottomSpacing = AppSpacing.xs
    val TitleSubtitleSpacing = AppSpacing.xxs
}

enum class OrdersScreenRole(
    val title: String,
) {
    Dispatcher("Диспетчер"),
    Loader("Грузчик"),
}

@Composable
fun OrdersScreenHeader(
    title: String,
    subtitle: String?,
    role: OrdersScreenRole,
    onSearchClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = OrdersScreenHeaderDefaults.HorizontalPadding)
                .padding(top = OrdersScreenHeaderDefaults.HeaderTopPadding),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = role.title,
                style = AppTypography.labelMedium,
                color = AppColors.MutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(OrdersScreenHeaderDefaults.RoleContextBottomSpacing))
            Text(
                text = title,
                style = AppTypography.headlineSmall,
                color = AppColors.Foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(OrdersScreenHeaderDefaults.TitleSubtitleSpacing))
                Text(
                    text = subtitle,
                    style = AppTypography.bodySmall,
                    color = AppColors.MutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (onSearchClick != null) {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Поиск",
                    tint = AppColors.Foreground,
                )
            }
        }
        if (onNotificationsClick != null) {
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = "Уведомления",
                    tint = AppColors.Foreground,
                )
            }
        }
    }
}
