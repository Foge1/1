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
import com.loaderapp.domain.model.OrderModel
import com.loaderapp.domain.model.OrderStatusModel
import com.loaderapp.presentation.dispatcher.CreateOrderViewModel
import com.loaderapp.presentation.dispatcher.NavigationEvent
import com.loaderapp.ui.components.GradientTopBar
import com.loaderapp.ui.theme.GoldStar
import java.text.SimpleDateFormat
import java.util.*

/**
 * Полноэкранный экран создания заказа для диспетчера.
 *
 * ## Архитектурные решения
 *
 * **Собственная ViewModel** ([CreateOrderViewModel]): экран изолирован от
 * [DispatcherViewModel]. После сохранения заказа в Room, [DispatcherViewModel]
 * получает обновление автоматически через реактивный Flow — без прямой связи.
 *
 * **Навигация через [NavigationEvent]**: ViewModel эмитит `NavigateUp` по
 * [Channel], UI подписывается через [LaunchedEffect]. Channel гарантирует
 * доставку события ровно одному подписчику и не теряет его при перекомпозиции.
 *
 * **Сборка [OrderModel]**: происходит здесь, в единственном месте приложения.
 * ViewModel и навигация ничего не знают о полях формы.
 *
 * @param dispatcherId  ID текущего диспетчера, встраивается в создаваемый заказ
 * @param onBack        Навигация назад (кнопка «назад» и успешное создание)
 * @param viewModel     Hilt ViewModel экрана; по умолчанию создаётся автоматически
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    dispatcherId: Long,
    onBack: () -> Unit,
    viewModel: CreateOrderViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current

    // Подписка на one-shot навигационные события из ViewModel
    LaunchedEffect(viewModel) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                NavigationEvent.NavigateUp -> onBack()
            }
        }
    }

    // ── Состояние формы ───────────────────────────────────────────────────────
    var address by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var estimatedHours by remember { mutableIntStateOf(2) }
    var comment by remember { mutableStateOf("") }
    var requiredWorkers by remember { mutableIntStateOf(1) }
    var minWorkerRating by remember { mutableFloatStateOf(0f) }

    // ── Состояние валидации ───────────────────────────────────────────────────
    var errorFields by remember { mutableStateOf(emptySet<String>()) }
    var showValidationBanner by remember { mutableStateOf(false) }

    // ── Дата / время ─────────────────────────────────────────────────────────
    val now = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableLongStateOf(now.timeInMillis) }
    var selectedHour by remember { mutableIntStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(now.get(Calendar.MINUTE)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale("ru")) }
    val primary = MaterialTheme.colorScheme.primary

    // ── Валидация ─────────────────────────────────────────────────────────────
    fun validate(): Boolean {
        val errors = buildSet {
            if (address.isBlank()) add("address")
            if (cargo.isBlank()) add("cargo")
            val p = price.toDoubleOrNull()
            if (p == null || p <= 0.0) add("price")
        }
        errorFields = errors
        showValidationBanner = errors.isNotEmpty()
        return errors.isEmpty()
    }

    // ── Сборка OrderModel — единственное место в приложении ──────────────────
    fun buildOrder(): OrderModel {
        val cal = Calendar.getInstance().apply {
            timeInMillis = selectedDate
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return OrderModel(
            id               = 0L,                             // присвоит БД
            address          = address.trim(),
            dateTime         = cal.timeInMillis,
            cargoDescription = cargo.trim(),
            pricePerHour     = price.toDouble(),
            estimatedHours   = estimatedHours,
            requiredWorkers  = requiredWorkers,
            minWorkerRating  = minWorkerRating.coerceIn(0f, 5f),
            status           = OrderStatusModel.AVAILABLE,
            createdAt        = System.currentTimeMillis(),
            completedAt      = null,
            workerId         = null,
            dispatcherId     = dispatcherId,
            workerRating     = null,
            comment          = comment.trim()
        )
    }

    // ── UI ───────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            GradientTopBar(
                title              = "Новый заказ",
                navigationIcon     = Icons.Default.ArrowBack,
                onNavigationClick  = onBack
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

            // Адрес
            AppField(
                icon           = Icons.Default.LocationOn,
                label          = "Адрес *",
                value          = address,
                onValueChange  = { address = it; errorFields = errorFields - "address" },
                placeholder    = "Например: ул. Ленина, 15",
                isError        = "address" in errorFields,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // Дата и время
            SectionLabel("Дата и время")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppPickerButton(
                    icon      = Icons.Default.DateRange,
                    label     = dateFormatter.format(Date(selectedDate)),
                    modifier  = Modifier.weight(1f),
                    onClick   = { showDatePicker = true }
                )
                AppPickerButton(
                    icon     = Icons.Default.AccessTime,
                    label    = "%02d:%02d".format(selectedHour, selectedMinute),
                    modifier = Modifier.weight(0.7f),
                    onClick  = { showTimePicker = true }
                )
            }

            // Описание груза
            AppField(
                icon            = Icons.Default.Inventory,
                label           = "Описание груза *",
                value           = cargo,
                onValueChange   = { cargo = it; errorFields = errorFields - "cargo" },
                placeholder     = "Что нужно перевезти",
                isError         = "cargo" in errorFields,
                maxLines        = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // Стоимость
            SectionLabel("Стоимость")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppField(
                    icon            = Icons.Default.CurrencyRuble,
                    label           = "₽/час *",
                    value           = price,
                    onValueChange   = { price = it; errorFields = errorFields - "price" },
                    placeholder     = "0",
                    isError         = "price" in errorFields,
                    modifier        = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingText     = "₽"
                )
                HoursStepper(
                    value      = estimatedHours,
                    onDecrement = { if (estimatedHours > 1) estimatedHours-- },
                    onIncrement = { if (estimatedHours < 24) estimatedHours++ },
                    modifier   = Modifier.weight(0.65f)
                )
            }

            // Итог
            val priceVal = price.toDoubleOrNull() ?: 0.0
            if (priceVal > 0.0 && estimatedHours > 0) {
                TotalRow(pricePerHour = priceVal, hours = estimatedHours, primary = primary)
            }

            // Количество грузчиков
            SectionLabel("Количество грузчиков")
            WorkerCountStepper(
                value      = requiredWorkers,
                onDecrement = { if (requiredWorkers > 1) requiredWorkers-- },
                onIncrement = { if (requiredWorkers < 50) requiredWorkers++ }
            )

            // Минимальный рейтинг
            SectionLabel("Минимальный рейтинг грузчика")
            RatingSlider(
                value       = minWorkerRating,
                onValueChange = { minWorkerRating = it },
                primary     = primary
            )

            // Комментарий
            AppField(
                icon            = Icons.Default.Comment,
                label           = "Комментарий диспетчера",
                value           = comment,
                onValueChange   = { comment = it },
                placeholder     = "Дополнительная информация для грузчика",
                maxLines        = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // Ошибка валидации
            if (showValidationBanner) {
                ValidationBanner()
            }

            // Кнопка создания
            Button(
                onClick = {
                    if (validate()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.createOrder(buildOrder())
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

    // Диалог выбора даты
    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) { DatePicker(state = dpState) }
    }

    // Диалог выбора времени
    if (showTimePicker) {
        val tpState = rememberTimePickerState(
            initialHour   = selectedHour,
            initialMinute = selectedMinute,
            is24Hour      = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour   = tpState.hour
                    selectedMinute = tpState.minute
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
private fun HoursStepper(
    value: Int,
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
                enabled  = value > 1,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Remove, null,
                    tint     = if (value > 1) primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                enabled  = value < 24,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Add, null,
                    tint     = if (value < 24) primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .background(primary.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = primary, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = primary)
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


