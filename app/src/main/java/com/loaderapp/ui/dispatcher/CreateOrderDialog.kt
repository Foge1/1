package com.loaderapp.ui.dispatcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel

data class CreateOrderFormState(
    val address: String = "",
    val cargoDescription: String = "",
    val pricePerHour: String = "",
    val estimatedHours: String = "",
    val requiredWorkers: String = "",
    val minWorkerRating: String = "3.0",
)

data class CreateOrderFormErrors(
    val address: Boolean = false,
    val cargo: Boolean = false,
    val price: Boolean = false,
    val hours: Boolean = false,
    val workers: Boolean = false,
)

/**
 * Диалог создания нового заказа
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderDialog(
    onDismiss: () -> Unit,
    onCreate: (OrderModel) -> Unit,
) {
    var state by remember { mutableStateOf(CreateOrderFormState()) }
    var errors by remember { mutableStateOf(CreateOrderFormErrors()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { HeaderSection() },
        text = {
            FormSection(
                state = state,
                errors = errors,
                onStateChanged = { state = it },
                onErrorsChanged = { errors = it },
            )
        },
        confirmButton = {
            ActionsSection(
                onConfirm = {
                    val validation = validate(state)
                    errors = validation
                    if (!validation.hasErrors()) {
                        onCreate(state.toOrderModel())
                    }
                },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun HeaderSection() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.Add, null)
        Text("Новый заказ")
    }
}

@Composable
private fun FormSection(
    state: CreateOrderFormState,
    errors: CreateOrderFormErrors,
    onStateChanged: (CreateOrderFormState) -> Unit,
    onErrorsChanged: (CreateOrderFormErrors) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AddressField(state = state, errors = errors, onStateChanged = onStateChanged, onErrorsChanged = onErrorsChanged)
        CargoField(state = state, errors = errors, onStateChanged = onStateChanged, onErrorsChanged = onErrorsChanged)
        PriceField(state = state, errors = errors, onStateChanged = onStateChanged, onErrorsChanged = onErrorsChanged)
        TimeWorkersSection(state = state, errors = errors, onStateChanged = onStateChanged, onErrorsChanged = onErrorsChanged)
        ValidationSection(
            minWorkerRating = state.minWorkerRating,
            onRatingChanged = { onStateChanged(state.copy(minWorkerRating = it)) },
        )
    }
}

@Composable
private fun AddressField(
    state: CreateOrderFormState,
    errors: CreateOrderFormErrors,
    onStateChanged: (CreateOrderFormState) -> Unit,
    onErrorsChanged: (CreateOrderFormErrors) -> Unit,
) {
    OutlinedTextField(
        value = state.address,
        onValueChange = {
            onStateChanged(state.copy(address = it))
            onErrorsChanged(errors.copy(address = false))
        },
        label = { Text("Адрес*") },
        leadingIcon = { Icon(Icons.Default.LocationOn, null) },
        isError = errors.address,
        supportingText = if (errors.address) ({ Text("Введите адрес") }) else null,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun CargoField(
    state: CreateOrderFormState,
    errors: CreateOrderFormErrors,
    onStateChanged: (CreateOrderFormState) -> Unit,
    onErrorsChanged: (CreateOrderFormErrors) -> Unit,
) {
    OutlinedTextField(
        value = state.cargoDescription,
        onValueChange = {
            onStateChanged(state.copy(cargoDescription = it))
            onErrorsChanged(errors.copy(cargo = false))
        },
        label = { Text("Описание груза*") },
        leadingIcon = { Icon(Icons.Default.Inventory, null) },
        isError = errors.cargo,
        supportingText = if (errors.cargo) ({ Text("Опишите груз") }) else null,
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 3,
    )
}

@Composable
private fun PriceField(
    state: CreateOrderFormState,
    errors: CreateOrderFormErrors,
    onStateChanged: (CreateOrderFormState) -> Unit,
    onErrorsChanged: (CreateOrderFormErrors) -> Unit,
) {
    OutlinedTextField(
        value = state.pricePerHour,
        onValueChange = {
            if (it.all(Char::isDigit) || it.isEmpty()) {
                onStateChanged(state.copy(pricePerHour = it))
                onErrorsChanged(errors.copy(price = false))
            }
        },
        label = { Text("Цена за час (₽)*") },
        leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = errors.price,
        supportingText = if (errors.price) ({ Text("Введите цену > 0") }) else null,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun TimeWorkersSection(
    state: CreateOrderFormState,
    errors: CreateOrderFormErrors,
    onStateChanged: (CreateOrderFormState) -> Unit,
    onErrorsChanged: (CreateOrderFormErrors) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.estimatedHours,
            onValueChange = {
                if (it.all(Char::isDigit) || it.isEmpty()) {
                    onStateChanged(state.copy(estimatedHours = it))
                    onErrorsChanged(errors.copy(hours = false))
                }
            },
            label = { Text("Часов*") },
            leadingIcon = { Icon(Icons.Default.Schedule, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = errors.hours,
            supportingText = if (errors.hours) ({ Text("≥1") }) else null,
            modifier = Modifier.weight(1f),
            singleLine = true,
        )

        OutlinedTextField(
            value = state.requiredWorkers,
            onValueChange = {
                if (it.all(Char::isDigit) || it.isEmpty()) {
                    onStateChanged(state.copy(requiredWorkers = it))
                    onErrorsChanged(errors.copy(workers = false))
                }
            },
            label = { Text("Грузчиков*") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = errors.workers,
            supportingText = if (errors.workers) ({ Text("≥1") }) else null,
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
    }
}

@Composable
private fun ValidationSection(
    minWorkerRating: String,
    onRatingChanged: (String) -> Unit,
) {
    Column {
        Text(
            text = "Минимальный рейтинг: $minWorkerRating",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = minWorkerRating.toFloatOrNull() ?: 3.0f,
            onValueChange = { onRatingChanged(String.format("%.1f", it)) },
            valueRange = 0f..5f,
            steps = 9,
        )
    }

    Text(
        text = "* Обязательные поля",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ActionsSection(onConfirm: () -> Unit) {
    Button(onClick = onConfirm) {
        Text("Создать")
    }
}

private fun validate(state: CreateOrderFormState): CreateOrderFormErrors =
    CreateOrderFormErrors(
        address = state.address.isBlank(),
        cargo = state.cargoDescription.isBlank(),
        price = state.pricePerHour.toIntOrNull()?.let { it <= 0 } ?: true,
        hours = state.estimatedHours.toIntOrNull()?.let { it < 1 } ?: true,
        workers = state.requiredWorkers.toIntOrNull()?.let { it < 1 } ?: true,
    )

private fun CreateOrderFormErrors.hasErrors(): Boolean = address || cargo || price || hours || workers

private fun CreateOrderFormState.toOrderModel(): OrderModel {
    val now = System.currentTimeMillis()
    return OrderModel(
        id = 0,
        address = address.trim(),
        dateTime = now,
        cargoDescription = cargoDescription.trim(),
        pricePerHour = pricePerHour.toDouble(),
        estimatedHours = estimatedHours.toInt(),
        requiredWorkers = requiredWorkers.toInt(),
        minWorkerRating = minWorkerRating.toFloat(),
        status = OrderStatusModel.AVAILABLE,
        createdAt = now,
        completedAt = null,
        workerId = null,
        dispatcherId = 0,
        workerRating = null,
        comment = "",
    )
}
