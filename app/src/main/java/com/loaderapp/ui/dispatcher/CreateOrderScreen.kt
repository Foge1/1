package com.loaderapp.ui.dispatcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.domain.model.OrderRules
import com.loaderapp.presentation.dispatcher.CreateOrderViewModel
import com.loaderapp.presentation.dispatcher.NavigationEvent
import com.loaderapp.presentation.dispatcher.OrderDayOption
import com.loaderapp.ui.components.GradientBackground
import com.loaderapp.ui.components.GradientTopBar
import com.loaderapp.ui.theme.GoldStar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class CreateOrderScreenFormState(
    val address: String = "",
    val cargo: String = "",
    val price: String = "",
    val comment: String = "",
    val requiredWorkers: Int = 1,
    val minWorkerRating: Float = 0f,
)

private data class CreateOrderUiState(
    val errorFields: Set<String> = emptySet(),
    val showValidationBanner: Boolean = false,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
)

private data class CreateOrderContentCallbacks(
    val onAddressChange: (String) -> Unit,
    val onDayOptionSelect: (OrderDayOption) -> Unit,
    val onTimePickerOpen: () -> Unit,
    val onCargoChange: (String) -> Unit,
    val onPriceChange: (String) -> Unit,
    val onHoursChange: (Int) -> Unit,
    val onRequiredWorkersChange: (Int) -> Unit,
    val onMinWorkerRatingChange: (Float) -> Unit,
    val onCommentChange: (String) -> Unit,
    val onCreate: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    onBack: () -> Unit,
    viewModel: CreateOrderViewModel = hiltViewModel(),
) {
    val haptic = LocalHapticFeedback.current
    val vmState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.navigationEvent.collect { event ->
            if (event == NavigationEvent.NavigateUp) onBack()
        }
    }

    var formState by remember { mutableStateOf(CreateOrderScreenFormState()) }
    var uiState by remember { mutableStateOf(CreateOrderUiState()) }

    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ru")) }
    val primary = MaterialTheme.colorScheme.primary
    val callbacks =
        CreateOrderContentCallbacks(
            onAddressChange = { value ->
                formState = formState.copy(address = value)
                uiState = uiState.copy(errorFields = uiState.errorFields - "address")
            },
            onDayOptionSelect = { option ->
                viewModel.onDayOptionSelected(option)
                if (option == OrderDayOption.OTHER_DATE) uiState = uiState.copy(showDatePicker = true)
            },
            onTimePickerOpen = { uiState = uiState.copy(showTimePicker = true) },
            onCargoChange = { value ->
                formState = formState.copy(cargo = value)
                uiState = uiState.copy(errorFields = uiState.errorFields - "cargo")
            },
            onPriceChange = { value ->
                formState = formState.copy(price = value)
                uiState = uiState.copy(errorFields = uiState.errorFields - "price")
            },
            onHoursChange = { newValue ->
                val current = vmState.estimatedHours
                when {
                    newValue > current -> repeat(newValue - current) { viewModel.incrementHours() }
                    newValue < current -> repeat(current - newValue) { viewModel.decrementHours() }
                }
            },
            onRequiredWorkersChange = { value -> formState = formState.copy(requiredWorkers = value) },
            onMinWorkerRatingChange = { value -> formState = formState.copy(minWorkerRating = value) },
            onCommentChange = { value -> formState = formState.copy(comment = value) },
            onCreate = {
                val validation = validateCreateOrderForm(formState)
                uiState = uiState.copy(errorFields = validation, showValidationBanner = validation.isNotEmpty())
                if (validation.isEmpty()) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.createOrder(
                        address = formState.address,
                        cargoDescription = formState.cargo,
                        pricePerHour = formState.price.toDouble(),
                        requiredWorkers = formState.requiredWorkers,
                        minWorkerRating = formState.minWorkerRating,
                        comment = formState.comment,
                    )
                }
            },
        )

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GradientTopBar(
                    title = "Новый заказ",
                    navigationIcon = Icons.Default.ArrowBack,
                    onNavigationClick = onBack,
                )
            },
        ) { padding ->
            CreateOrderContent(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            padding,
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .imePadding(),
                vmState = vmState,
                formState = formState,
                uiState = uiState,
                primary = primary,
                dateFormatter = dateFormatter,
                callbacks = callbacks,
            )
        }
    }

    if (uiState.showDatePicker) {
        CreateOrderDateDialog(
            selectedDateMillis = vmState.selectedDateMillis,
            onDismiss = { uiState = uiState.copy(showDatePicker = false) },
            onConfirm = { millis ->
                viewModel.onDateSelected(millis)
                uiState = uiState.copy(showDatePicker = false)
            },
        )
    }

    if (uiState.showTimePicker) {
        CreateOrderTimeDialog(
            hour = vmState.selectedHour,
            minute = vmState.selectedMinute,
            onDismiss = { uiState = uiState.copy(showTimePicker = false) },
            onConfirm = { hour, minute ->
                viewModel.onTimeSelected(hour, minute)
                uiState = uiState.copy(showTimePicker = false)
            },
        )
    }
}

@Composable
private fun CreateOrderContent(
    modifier: Modifier,
    vmState: com.loaderapp.presentation.dispatcher.CreateOrderUiState,
    formState: CreateOrderScreenFormState,
    uiState: CreateOrderUiState,
    primary: androidx.compose.ui.graphics.Color,
    dateFormatter: SimpleDateFormat,
    callbacks: CreateOrderContentCallbacks,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppField(
            icon = Icons.Default.LocationOn,
            label = "Адрес *",
            value = formState.address,
            onValueChange = callbacks.onAddressChange,
            placeholder = "Например: ул. Ленина, 15",
            isError = "address" in uiState.errorFields,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        SectionLabel("Дата и время")
        DayOptionSelector(selected = vmState.selectedDayOption, onSelect = callbacks.onDayOptionSelect)

        DateTimeSection(vmState = vmState, dateFormatter = dateFormatter, onOpenTimePicker = callbacks.onTimePickerOpen)

        AppField(
            icon = Icons.Default.Inventory,
            label = "Описание груза *",
            value = formState.cargo,
            onValueChange = callbacks.onCargoChange,
            placeholder = "Например: Переезд, мебель, коробки",
            isError = "cargo" in uiState.errorFields,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        SectionLabel("Стоимость")
        AppField(
            icon = Icons.Default.AttachMoney,
            label = "Цена за час *",
            value = formState.price,
            onValueChange = { input -> if (input.all { it.isDigit() } || input.isEmpty()) callbacks.onPriceChange(input) },
            placeholder = "0",
            leadingText = "₽",
            isError = "price" in uiState.errorFields,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        HoursSelector(value = vmState.estimatedHours, onValueChange = callbacks.onHoursChange)

        if (vmState.estimatedHours < OrderRules.MIN_ESTIMATED_HOURS) {
            Text(
                text = "Минимальный заказ — от ${OrderRules.MIN_ESTIMATED_HOURS} часов",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        val priceVal = formState.price.toDoubleOrNull() ?: 0.0
        if (priceVal > 0.0) {
            TotalRow(pricePerHour = priceVal, hours = vmState.estimatedHours, primary = primary)
        }

        SectionLabel("Количество грузчиков")
        WorkerCountStepper(
            value = formState.requiredWorkers,
            onDecrement = { if (formState.requiredWorkers > 1) callbacks.onRequiredWorkersChange(formState.requiredWorkers - 1) },
            onIncrement = { if (formState.requiredWorkers < 50) callbacks.onRequiredWorkersChange(formState.requiredWorkers + 1) },
        )

        SectionLabel("Минимальный рейтинг грузчика")
        RatingSlider(value = formState.minWorkerRating, onValueChange = callbacks.onMinWorkerRatingChange, primary = primary)

        AppField(
            icon = Icons.Default.Comment,
            label = "Комментарий диспетчера",
            value = formState.comment,
            onValueChange = callbacks.onCommentChange,
            placeholder = "Дополнительная информация для грузчика",
            maxLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        if (uiState.showValidationBanner) ValidationBanner()

        Button(
            onClick = callbacks.onCreate,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Создать заказ", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DateTimeSection(
    vmState: com.loaderapp.presentation.dispatcher.CreateOrderUiState,
    dateFormatter: SimpleDateFormat,
    onOpenTimePicker: () -> Unit,
) {
    val selectedTimeLabel = "%02d:%02d".format(vmState.selectedHour, vmState.selectedMinute)

    if (vmState.selectedDayOption == OrderDayOption.SOON) {
        Text("Режим: Ближайшее время", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    Text(
        text = "Дата: ${dateFormatter.format(Date(vmState.selectedDateMillis))}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Text(
        text = "Время: $selectedTimeLabel",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )

    AppPickerButton(
        icon = Icons.Default.Schedule,
        label = selectedTimeLabel,
        onClick = onOpenTimePicker,
    )
}

@Composable
private fun CreateOrderDateDialog(
    selectedDateMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val dpState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                dpState.selectedDateMillis?.let(onConfirm)
            }) { Text("ОК") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    ) { DatePicker(state = dpState) }
}

@Composable
private fun CreateOrderTimeDialog(
    hour: Int,
    minute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val tpState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(tpState.hour, tpState.minute) }) { Text("ОК") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        text = { TimePicker(state = tpState) },
    )
}

private fun validateCreateOrderForm(state: CreateOrderScreenFormState): Set<String> =
    buildSet {
        if (state.address.isBlank()) add("address")
        if (state.cargo.isBlank()) add("cargo")
        val parsedPrice = state.price.toDoubleOrNull()
        if (parsedPrice == null || parsedPrice <= 0.0) add("price")
    }

// ── Приватные UI-компоненты экрана ────────────────────────────────────────────

@Composable
private fun DayOptionSelector(
    selected: OrderDayOption,
    onSelect: (OrderDayOption) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth(),
    ) {
        DayOption.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option.value,
                onClick = { onSelect(option.value) },
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = DayOption.entries.size,
                    ),
                modifier = Modifier.weight(1f),
                label = {
                    Text(
                        text = option.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

private enum class DayOption(
    val value: OrderDayOption,
    val label: String,
) {
    TODAY(OrderDayOption.TODAY, "Сегодня"),
    TOMORROW(OrderDayOption.TOMORROW, "Завтра"),
    SOON(OrderDayOption.SOON, "Ближайшее"),
    OTHER_DATE(OrderDayOption.OTHER_DATE, "Дата"),
}

@Composable
private fun HoursSelector(
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    val minValue = OrderRules.MIN_ESTIMATED_HOURS
    val maxValue = OrderRules.MAX_ESTIMATED_HOURS
    val primary = MaterialTheme.colorScheme.primary

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Часов",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = primary,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp).fillMaxWidth(),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, primary.copy(0.5f), RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { onValueChange(value - 1) }, enabled = value > minValue, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Remove,
                    null,
                    tint =
                        if (value >
                            minValue
                        ) {
                            primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(text = "$value", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = primary)
            IconButton(onClick = { onValueChange(value + 1) }, enabled = value < maxValue, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Add,
                    null,
                    tint =
                        if (value <
                            maxValue
                        ) {
                            primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun TotalRow(
    pricePerHour: Double,
    hours: Int,
    primary: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(primary.copy(alpha = 0.08f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Итого за ~$hours ч:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "${(pricePerHour * hours).toInt()} ₽",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = primary,
        )
    }
}

@Composable
private fun WorkerCountStepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onDecrement, enabled = value > 1, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Remove, null, tint = if (value > 1) primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = primary)
            Text("чел.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onIncrement, enabled = value < 50, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Add, null, tint = if (value < 50) primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RatingSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    primary: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Default.Star, null, tint = GoldStar, modifier = Modifier.size(20.dp))
            Text(
                text = if (value == 0f) "Без ограничений" else "от ${"%.1f".format(value)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (value == 0f) MaterialTheme.colorScheme.onSurfaceVariant else primary,
                modifier = Modifier.weight(1f),
            )
            if (value > 0f) {
                TextButton(
                    onClick = { onValueChange(0f) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) { Text("Сбросить", fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = { onValueChange((Math.round(it * 10) / 10f)) },
            valueRange = 0f..5f,
            steps = 49,
            colors =
                SliderDefaults.colors(
                    thumbColor = primary,
                    activeTrackColor = primary,
                ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("0", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("5.0", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ValidationBanner() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.Error,
            null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "Заполните все обязательные поля",
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
        )
    }
}

// ── Переиспользуемые поля формы ───────────────────────────────────────────────

/**
 * Стилизованное поле ввода в дизайне приложения.
 * Используется только на экране создания заказа.
 */
@Composable
fun AppField(
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isError: Boolean = false,
    maxLines: Int = 1,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingText: String? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderColor = appFieldBorderColor(isError = isError, hasValue = value.isNotEmpty(), primary = primary)

    Column(modifier = modifier) {
        AppFieldLabel(label = label, isError = isError, primary = primary)
        AppFieldContainer(
            borderColor = borderColor,
            maxLines = maxLines,
            icon = icon,
            leadingText = leadingText,
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            keyboardOptions = keyboardOptions,
            primary = primary,
        )
        if (isError) {
            Text(
                "Обязательное поле",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 3.dp, start = 4.dp),
            )
        }
    }
}

@Composable
private fun AppFieldLabel(
    label: String,
    isError: Boolean,
    primary: androidx.compose.ui.graphics.Color,
) {
    Text(
        text = label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isError) MaterialTheme.colorScheme.error else primary,
        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp),
    )
}

@Composable
private fun AppFieldContainer(
    borderColor: androidx.compose.ui.graphics.Color,
    maxLines: Int,
    icon: ImageVector,
    leadingText: String?,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions,
    primary: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = if (maxLines > 1) 10.dp else 4.dp),
        verticalAlignment = if (maxLines > 1) Alignment.Top else Alignment.CenterVertically,
    ) {
        AppFieldLeading(icon = icon, leadingText = leadingText, value = value, maxLines = maxLines, primary = primary)
        Spacer(Modifier.width(10.dp))
        AppFieldInput(
            value = value,
            onValueChange = onValueChange,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            placeholder = placeholder,
        )
    }
}

@Composable
private fun AppFieldLeading(
    icon: ImageVector,
    leadingText: String?,
    value: String,
    maxLines: Int,
    primary: androidx.compose.ui.graphics.Color,
) {
    val color = if (value.isNotEmpty()) primary else MaterialTheme.colorScheme.onSurfaceVariant
    val topPadding = if (maxLines > 1) 4.dp else 0.dp
    if (leadingText != null) {
        Text(
            text = leadingText,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(top = topPadding),
        )
    } else {
        Icon(
            icon,
            null,
            tint = color,
            modifier = Modifier.size(20.dp).padding(top = topPadding),
        )
    }
}

@Composable
private fun AppFieldInput(
    value: String,
    onValueChange: (String) -> Unit,
    maxLines: Int,
    keyboardOptions: KeyboardOptions,
    placeholder: String,
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        maxLines = maxLines,
        singleLine = maxLines == 1,
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun appFieldBorderColor(
    isError: Boolean,
    hasValue: Boolean,
    primary: androidx.compose.ui.graphics.Color,
): androidx.compose.ui.graphics.Color =
    when {
        isError -> MaterialTheme.colorScheme.error
        hasValue -> primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

/**
 * Кнопка-пикер для выбора даты или времени.
 */
@Composable
fun AppPickerButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .background(primary.copy(alpha = 0.06f))
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = if (enabled) primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}
