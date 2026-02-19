package com.loaderapp.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loaderapp.ui.components.LoadingView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран деталей заказа (упрощенная версия)
 * TODO: Добавить ViewModel для загрузки заказа по ID
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Long,
    isDispatcher: Boolean,
    onBack: () -> Unit
) {
    // Placeholder - в реальности здесь должен быть ViewModel
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Детали заказа #$orderId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Order ID: $orderId",
                style = MaterialTheme.typography.titleLarge
            )
            
            Text(
                text = if (isDispatcher) "Просмотр как диспетчер" else "Просмотр как грузчик",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // TODO: Загрузить детали заказа через ViewModel
            LoadingView(message = "Экран в разработке...")
        }
    }
}
