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

/**
 * Компонент пустого состояния
 */
@Composable
fun EmptyStateView(
    icon: ImageVector = Icons.Default.Search,
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .alpha(0.4f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        if (message != null) {
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "Ошибка",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(20.dp)
                    .alpha(alpha)
                    .shimmerBackground()
            )
            
            // Описание
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(16.dp)
                    .alpha(alpha)
                    .shimmerBackground()
            )
            
            Spacer(Modifier.height(4.dp))
            
            // Параметры
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(16.dp)
                            .alpha(alpha)
                            .shimmerBackground()
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.shimmerBackground(): Modifier = this.then(
    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
)


// ════════════════════════════════════════════════════════════════════════════
// Общие компоненты для карточек заказов (DRY — убираем дублирование)
// Использовались в LoaderScreen и DispatcherScreen независимо.
// ════════════════════════════════════════════════════════════════════════════

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.loaderapp.domain.model.OrderStatusModel
import java.text.SimpleDateFormat
import java.util.*

@androidx.compose.runtime.Composable
fun OrderStatusChip(status: OrderStatusModel) {
    val (text, color) = when (status) {
        OrderStatusModel.AVAILABLE  -> "Доступен"  to Color(0xFF4CAF50)
        OrderStatusModel.TAKEN      -> "Взят"      to Color(0xFFFF9800)
        OrderStatusModel.IN_PROGRESS-> "В работе"  to Color(0xFF2196F3)
        OrderStatusModel.COMPLETED  -> "Завершён"  to Color(0xFF9C27B0)
        OrderStatusModel.CANCELLED  -> "Отменён"   to Color(0xFFF44336)
    }
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@androidx.compose.runtime.Composable
fun OrderParamItem(icon: ImageVector, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun formatOrderDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale("ru"))
    return sdf.format(Date(timestamp))
}
