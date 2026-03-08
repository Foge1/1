package com.loaderapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.core.ui.theme.ShapeChip
import com.loaderapp.ui.theme.LoaderAppTheme

@Composable
fun MetaChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.semantics { contentDescription = text },
        color = AppColors.Muted,
        shape = ShapeChip,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = AppSpacing.sm,
                    vertical = AppSpacing.xs,
                ),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AppSpacing.sm),
                tint = AppColors.MutedForeground,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.MutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MetaChipPreview() {
    LoaderAppTheme {
        MetaChip(
            icon = Icons.Filled.Info,
            text = "Пример мета-информации",
        )
    }
}
