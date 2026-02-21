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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.domain.model.OrderRules
import com.loaderapp.presentation.dispatcher.CreateOrderViewModel
import com.loaderapp.presentation.dispatcher.NavigationEvent
import com.loaderapp.presentation.dispatcher.OrderDayOption
import com.loaderapp.ui.components.GradientTopBar
import com.loaderapp.ui.theme.GoldStar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    dispatcherId: Long,
    onBack: () -> Unit,
    viewModel: CreateOrderViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                NavigationEvent.NavigateUp -> onBack()
            }
        }
    }

    var address by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var requiredWorkers by remember { mutableIntStateOf(1) }
    var minWorkerRating by remember { mutableFloatStateOf(0f) }

    var errorFields by remember { mutableStateOf(emptySet<String>()) }
    var showValidationBanner by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ru")) }
    val primary = MaterialTheme.colorScheme.primary

    fun validate(): Boolean {
        val errors = buildSet {
            if (address.isBlank()) add("address")
            if (cargo.isBlank()) add("cargo")
            val parsedPrice = price.toDoubleOrNull()
            if (parsedPrice == null || parsedPrice <= 0.0) add("price")
        }
        errorFields = errors
        showValidationBanner = errors.isNotEmpty()
        return errors.isEmpty()
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Новый заказ",
                navigationIcon = Icons.Default.ArrowBack,
                onNavigationClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppField(
                icon = Icons.Default.LocationOn,
                label = "Адрес *",
                value = address,
                onValueChange = { address = it; errorFields = errorFields - "address" },
                placeholder = "Например: ул. Ленина, 15",
                isError = "address" in errorFields,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            SectionLabel("Дата и время")
            DayOptionSelector(
                selected = uiState.selectedDayOption,
                onSelect = { option ->
                    viewModel.onDayOptionSelected(option)
                    if (option == OrderDayOption.OTHER_DATE) showDatePicker = true
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppPickerButton(
                    icon = Icons.Default.DateRange,
                    label = dateFormatter.format(Date(uiState.selectedDateMillis)),
                    modifier = Modifier.weight(1f),
                    onClick = { showDatePicker = true },
                    enabled = uiState.selectedDayOption == OrderDayOption.OTHER_DATE
                )
                AppPickerButton(
                    icon = Icons.Default.AccessTime,
                    label = "%02d:%02d".format(uiState.selectedHour, uiState.selectedMinute),
                    modifier = Modifier.weight(0.7f),
                    onClick = { showTimePicker = true }
                )
            }

            AppField(
                icon = Icons.Default.Inventory,
                label = "Описание груза *",
                value = cargo,
                onValueChange = { cargo = it; errorFields = errorFields - "cargo" },
                placeholder = "Что нужно перевезти",
                isError = "cargo" in errorFields,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            SectionLabel("Стоимость")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppField(
                    icon = Icons.Default.CurrencyRuble,
                    label = "₽/час *",
                    value = price,
                    onValueChange = { price = it; errorFields = errorFields - "price" },
                    placeholder = "0",
                    isError = "price" in errorFields,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingText = "₽"
                )
                HoursStepper(
                    value = uiState.estimatedHours,
                    minValue = OrderRules.MIN_ESTIMATED_HOURS,
                    maxValue = OrderRules.MAX_ESTIMATED_HOURS,
                    onDecrement = viewModel::decrementHours,
                    onIncrement = viewModel::incrementHours,
                    modifier = Modifier.weight(0.65f)
                )
            }
            Text(
                text = "Минимальный заказ — от ${OrderRules.MIN_ESTIMATED_HOURS} часов",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = (-10).dp, start = 4.dp)
            )

            val priceVal = price.toDoubleOrNull() ?: 0.0
            if (priceVal > 0.0) {
                TotalRow(pricePerHour = priceVal, hours = uiState.estimatedHours, primary = primary)
            }

            SectionLabel("Количество грузчиков")
            WorkerCountStepper(
                value = requiredWorkers,
                onDecrement = { if (requiredWorkers > 1) requiredWorkers-- },
                onIncrement = { if (requiredWorkers < 50) requiredWorkers++ }
            )

            SectionLabel("Минимальный рейтинг грузчика")
            RatingSlider(
                value = minWorkerRating,
                onValueChange = { minWorkerRating = it },
                primary = primary
            )

            AppField(
                icon = Icons.Default.Comment,
                label = "Комментарий диспетчера",
                value = comment,
                onValueChange = { comment = it },
                placeholder = "Дополнительная информация для грузчика",
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            if (showValidationBanner) {
                ValidationBanner()
            }

            Button(
                onClick = {
                    if (validate()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.createOrder(
                            dispatcherId = dispatcherId,
                            address = address,
                            cargoDescription = cargo,
                            pricePerHour = price.toDouble(),
                            requiredWorkers = requiredWorkers,
                            minWorkerRating = minWorkerRating,
                            comment = comment
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Создать заказ", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = uiState.selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let(viewModel::onDateSelected)
                    showDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) { DatePicker(state = dpState) }
    }

    if (showTimePicker) {
        val tpState = rememberTimePickerState(
            initialHour = uiState.selectedHour,
            initialMinute = uiState.selectedMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onTimeSelected(tpState.hour, tpState.minute)
                    showTimePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Отмена") }
            },
            text = { TimePicker(state = tpState) }
        )
    }
}

// ── Приватные UI-компоненты экрана ────────────────────────────────────────────

@Composable
private fun DayOptionSelector(
    selected: OrderDayOption,
    onSelect: (OrderDayOption) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        DayOption.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option.value,
                onClick = { onSelect(option.value) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = DayOption.entries.size
                ),
                label = { Text(option.label) }
            )
        }
    }
}

private enum class DayOption(val value: OrderDayOption, val label: String) {
    TODAY(OrderDayOption.TODAY, "Сегодня"),
    TOMORROW(OrderDayOption.TOMORROW, "Завтра"),
    OTHER_DATE(OrderDayOption.OTHER_DATE, "Другая дата")
}

@Composable
private fun HoursStepper(
    value: Int,
    minValue: Int,
    maxValue: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier             = modifier,
        horizontalAlignment  = Alignment.CenterHorizontally
    ) {
        Text(
            text       = "Часов",
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = primary,
            modifier   = Modifier
                .padding(bottom = 6.dp, start = 2.dp)
                .fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, primary.copy(0.5f), RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick  = onDecrement,
                enabled  = value > minValue,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Remove, null,
                    tint     = if (value > minValue) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text       = "$value",
                fontSize   = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = primary
            )
            IconButton(
                onClick  = onIncrement,
                enabled  = value < maxValue,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Add, null,
                    tint     = if (value < maxValue) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TotalRow(pricePerHour: Double, hours: Int, primary: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(primary.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            "Итого за ~$hours ч:",
            fontSize = 14.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text       = "${(pricePerHour * hours).toInt()} ₽",
            fontSize   = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = primary
        )
    }
}

@Composable
private fun WorkerCountStepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
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
    primary: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Star, null, tint = GoldStar, modifier = Modifier.size(20.dp))
            Text(
                text       = if (value == 0f) "Без ограничений" else "от ${"%.1f".format(value)}",
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (value == 0f) MaterialTheme.colorScheme.onSurfaceVariant else primary,
                modifier   = Modifier.weight(1f)
            )
            if (value > 0f) {
                TextButton(
                    onClick         = { onValueChange(0f) },
                    contentPadding  = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) { Text("Сбросить", fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value          = value,
            onValueChange  = { onValueChange((Math.round(it * 10) / 10f)) },
            valueRange     = 0f..5f,
            steps          = 49,
            colors         = SliderDefaults.colors(
                thumbColor       = primary,
                activeTrackColor = primary
            )
        )
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("5.0", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ValidationBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Error, null,
            tint     = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Text(
            "Заполните все обязательные поля",
            color    = MaterialTheme.colorScheme.error,
            fontSize = 13.sp
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
    leadingText: String? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderColor = when {
        isError        -> MaterialTheme.colorScheme.error
        value.isNotEmpty() -> primary
        else           -> MaterialTheme.colorScheme.outlineVariant
    }

    Column(modifier = modifier) {
        Text(
            text       = label,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (isError) MaterialTheme.colorScheme.error else primary,
            modifier   = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(
                    horizontal = 12.dp,
                    vertical   = if (maxLines > 1) 10.dp else 4.dp
                ),
            verticalAlignment = if (maxLines > 1) Alignment.Top else Alignment.CenterVertically
        ) {
            if (leadingText != null) {
                Text(
                    text       = leadingText,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (value.isNotEmpty()) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier   = Modifier.padding(top = if (maxLines > 1) 4.dp else 0.dp)
                )
            } else {
                Icon(
                    icon, null,
                    tint     = if (value.isNotEmpty()) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = if (maxLines > 1) 4.dp else 0.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            androidx.compose.foundation.text.BasicTextField(
                value          = value,
                onValueChange  = onValueChange,
                maxLines       = maxLines,
                singleLine     = maxLines == 1,
                keyboardOptions = keyboardOptions,
                textStyle      = MaterialTheme.typography.bodyLarge.copy(
                    color    = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp
                ),
                modifier       = Modifier.fillMaxWidth(),
                decorationBox  = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                fontSize = 15.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        inner()
                    }
                }
            )
        }
        if (isError) {
            Text(
                "Обязательное поле",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 3.dp, start = 4.dp)
            )
        }
    }
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
    enabled: Boolean = true
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .background(primary.copy(alpha = 0.06f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = if (enabled) primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (enabled) primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        fontSize   = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.primary,
        modifier   = Modifier.padding(bottom = 2.dp)
    )
}


