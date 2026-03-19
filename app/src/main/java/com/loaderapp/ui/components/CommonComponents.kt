package com.loaderapp.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppSpacing

@Composable
fun EmptyStateView(
    icon: ImageVector = Icons.Default.Search,
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
) {
    StateMessageView(
        icon = icon,
        iconTint = AppColors.MutedForeground,
        title = title,
        subtitle = message,
        modifier = modifier,
    )
}

@Composable
fun ErrorView(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    StateMessageView(
        icon = Icons.Default.Error,
        iconTint = AppColors.Destructive,
        title = "Ошибка",
        subtitle = message,
        modifier = modifier,
        action =
            if (onRetry != null) {
                {
                    OutlinedButton(
                        onClick = onRetry,
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = AppColors.Destructive,
                            ),
                    ) {
                        Text("Повторить")
                    }
                }
            } else {
                null
            },
    )
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = AppColors.Primary,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun StateMessageView(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .background(
                        color = AppColors.Muted,
                        shape = MaterialTheme.shapes.extraLarge,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconTint,
            )
        }

        Spacer(Modifier.height(AppSpacing.lg))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(AppSpacing.sm))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.MutedForeground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = AppSpacing.xxxl),
            )
        }

        if (action != null) {
            Spacer(Modifier.height(AppSpacing.lg))
            action()
        }
    }
}

@Composable
fun SkeletonOrderCard(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "shimmer_alpha",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(AppSpacing.xl)
                        .alpha(alpha)
                        .shimmerBackground(),
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.9f)
                        .height(AppSpacing.lg)
                        .alpha(alpha)
                        .shimmerBackground(),
            )

            Spacer(Modifier.height(AppSpacing.xs))

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(4) {
                    Box(
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(AppSpacing.lg)
                                .alpha(alpha)
                                .shimmerBackground(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.shimmerBackground(): Modifier =
    this.then(
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    )
