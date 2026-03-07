package com.loaderapp.core.ui.components.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.ShapeButton

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(AppButtonDefaults.IconButtonSize),
        shape = ShapeButton,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = AppColors.Primary,
                contentColor = AppColors.OnPrimary,
                disabledContainerColor = AppButtonDefaults.disabledContainerColor(AppColors.Primary),
                disabledContentColor = AppButtonDefaults.disabledContentColor(AppColors.OnPrimary),
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(AppButtonDefaults.IconSize),
        )
    }
}
