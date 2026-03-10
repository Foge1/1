package com.loaderapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppShapes
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.core.ui.theme.AppTypography

private object RoleSelectorDefaults {
    val HorizontalPadding = AppSpacing.lg
    val ContentPadding = AppSpacing.sm
    val ItemSpacing = AppSpacing.xs
    const val SelectedContainerAlpha = 0.16f
    val BorderWidth = AppSpacing.xxs / 2
}

@Composable
fun RoleSelector(
    currentRole: OrdersScreenRole,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = RoleSelectorDefaults.HorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(RoleSelectorDefaults.ItemSpacing),
    ) {
        OrdersScreenRole.entries.forEach { role ->
            val isSelected = role == currentRole
            Surface(
                modifier = Modifier.weight(1f),
                shape = AppShapes.medium,
                color =
                    if (isSelected) {
                        AppColors.Primary.copy(alpha = RoleSelectorDefaults.SelectedContainerAlpha)
                    } else {
                        AppColors.Surface
                    },
                border = BorderStroke(
                    width = RoleSelectorDefaults.BorderWidth,
                    color = if (isSelected) AppColors.Primary else AppColors.Border,
                ),
            ) {
                Text(
                    text = role.title,
                    style = AppTypography.labelLarge,
                    color = if (isSelected) AppColors.Primary else AppColors.MutedForeground,
                    modifier = Modifier.padding(RoleSelectorDefaults.ContentPadding),
                )
            }
        }
    }
}
