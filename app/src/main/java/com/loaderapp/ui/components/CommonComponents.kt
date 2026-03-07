package com.loaderapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.loaderapp.core.ui.theme.AppSpacing

/**
 * Компонент пустого состояния
 */
@Composable
fun EmptyStateView(
    icon: ImageVector = Icons.Default.Search,
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier =
                Modifier
                    .size(80.dp)
                    .alpha(0.4f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(AppSpacing.lg))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        if (message != null) {
            Spacer(Modifier.height(AppSpacing.sm))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = AppSpacing.xxxl),
            )
        }
    }
}

/**
 * Компонент ошибки
 */
@Composable
fun ErrorView(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error,
        )

        Spacer(Modifier.height(AppSpacing.lg))

        Text(
            text = "Ошибка",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )

        Spacer(Modifier.height(AppSpacing.sm))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AppSpacing.xxxl),
        )

        if (onRetry != null) {
            Spacer(Modifier.height(AppSpacing.lg))

            Button(onClick = onRetry) {
                Text("Повторить")
            }
        }
    }
}

/**
 * Компонент загрузки
 */
@Composable
fun LoadingView(
    message: String = "Загрузка...",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(AppSpacing.lg))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Скелетон для карточки заказа (shimmer эффект)
 */
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
            // Заголовок
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(AppSpacing.xl)
                        .alpha(alpha)
                        .shimmerBackground(),
            )

            // Описание
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(0.9f)
                        .height(AppSpacing.lg)
                        .alpha(alpha)
                        .shimmerBackground(),
            )

            Spacer(Modifier.height(AppSpacing.xs))

            // Параметры
            Row(
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
