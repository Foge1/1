package com.loaderapp.ui.dispatcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel

/**
 * Диалог создания нового заказа
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderDialog(
    onDismiss: () -> Unit,
    onCreate: (OrderModel) -> Unit
) {
    var address by remember { mutableStateOf("") }
    var cargoDescription by remember { mutableStateOf("") }
    var pricePerHour by remember { mutableStateOf("") }
    var estimatedHours by remember { mutableStateOf("") }
    var requiredWorkers by remember { mutableStateOf("") }
    var minWorkerRating by remember { mutableStateOf("3.0") }
    
    var addressError by remember { mutableStateOf(false) }
    var cargoError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }
    var hoursError by remember { mutableStateOf(false) }
    var workersError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Text("Новый заказ")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Адрес
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        addressError = false
                    },
                    label = { Text("Адрес*") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    isError = addressError,
                    supportingText = if (addressError) {
                        { Text("Введите адрес") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Описание груза
                OutlinedTextField(
                    value = cargoDescription,
                    onValueChange = {
                        cargoDescription = it
                        cargoError = false
                    },
                    label = { Text("Описание груза*") },
                    leadingIcon = { Icon(Icons.Default.Inventory, null) },
                    isError = cargoError,
                    supportingText = if (cargoError) {
                        { Text("Опишите груз") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
                
                // Цена за час
                OutlinedTextField(
                    value = pricePerHour,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } || it.isEmpty()) {
                            pricePerHour = it
                            priceError = false
                        }
                    },
                    label = { Text("Цена за час (₽)*") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = priceError,
                    supportingText = if (priceError) {
                        { Text("Введите цену > 0") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Часы работы
                    OutlinedTextField(
                        value = estimatedHours,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } || it.isEmpty()) {
                                estimatedHours = it
                                hoursError = false
                            }
                        },
                        label = { Text("Часов*") },
                        leadingIcon = { Icon(Icons.Default.Schedule, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = hoursError,
                        supportingText = if (hoursError) {
                            { Text("≥1") }
                        } else null,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    // Количество грузчиков
                    OutlinedTextField(
                        value = requiredWorkers,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } || it.isEmpty()) {
                                requiredWorkers = it
                                workersError = false
                            }
                        },
                        label = { Text("Грузчиков*") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = workersError,
                        supportingText = if (workersError) {
                            { Text("≥1") }
                        } else null,
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                // Минимальный рейтинг
                Column {
                    Text(
                        text = "Минимальный рейтинг: ${minWorkerRating}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = minWorkerRating.toFloatOrNull() ?: 3.0f,
                        onValueChange = { minWorkerRating = String.format("%.1f", it) },
                        valueRange = 0f..5f,
                        steps = 9 // 0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4, 4.5, 5
                    )
                }
                
                Text(
                    text = "* Обязательные поля",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Валидация
                    var hasErrors = false
                    
                    if (address.isBlank()) {
                        addressError = true
                        hasErrors = true
                    }
                    if (cargoDescription.isBlank()) {
                        cargoError = true
                        hasErrors = true
                    }
                    if (pricePerHour.toIntOrNull() == null || pricePerHour.toInt() <= 0) {
                        priceError = true
                        hasErrors = true
                    }
                    if (estimatedHours.toIntOrNull() == null || estimatedHours.toInt() < 1) {
                        hoursError = true
                        hasErrors = true
                    }
                    if (requiredWorkers.toIntOrNull() == null || requiredWorkers.toInt() < 1) {
                        workersError = true
                        hasErrors = true
                    }
                    
                    if (!hasErrors) {
                        val order = OrderModel(
                            id = 0, // Будет установлен БД
                            address = address.trim(),
                            dateTime = System.currentTimeMillis(),
                            cargoDescription = cargoDescription.trim(),
                            pricePerHour = pricePerHour.toDouble(),
                            estimatedHours = estimatedHours.toInt(),
                            requiredWorkers = requiredWorkers.toInt(),
                            minWorkerRating = minWorkerRating.toFloat(),
                            status = OrderStatusModel.AVAILABLE,
                            createdAt = System.currentTimeMillis(),
                            completedAt = null,
                            workerId = null,
                            dispatcherId = 0, // Будет установлен ViewModel
                            workerRating = null,
                            comment = ""
                        )
                        onCreate(order)
                    }
                }
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
