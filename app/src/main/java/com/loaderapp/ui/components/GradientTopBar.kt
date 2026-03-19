package com.loaderapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.loaderapp.core.ui.theme.AppSpacing

private object GradientTopBarDefaults {
    val OverlayHeight = AppSpacing.xxxl * 3
    val TitleStartPaddingWithNavigation = AppSpacing.xxxl + AppSpacing.xl + AppSpacing.xs
}

@Composable
fun GradientTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(GradientTopBarDefaults.OverlayHeight)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(appScreenBackgroundBottomColor(), Color.Transparent),
                    ),
                ),
    )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = AppSpacing.xs, vertical = AppSpacing.sm),
    ) {
        if (navigationIcon != null) {
            IconButton(
                onClick = onNavigationClick,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = navigationIcon,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(
                        start = if (navigationIcon != null) GradientTopBarDefaults.TitleStartPaddingWithNavigation else AppSpacing.md,
                        end = GradientTopBarDefaults.TitleStartPaddingWithNavigation,
                    ),
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}
