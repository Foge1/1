package com.loaderapp.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.presentation.profile.ProfileStats
import com.loaderapp.presentation.profile.ProfileViewModel
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.GradientTopBar
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.scrollableGradientBackground
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран профиля пользователя (Диспетчер и Грузчик).
 *
 * Архитектура фона:
 * - Весь экран — один [Box], заполняющий [fillMaxSize].
 * - Контент ([ProfileContent]) — [Column] с [scrollableGradientBackground]:
 *   градиент привязан к длине контента (endY = INFINITY), а не viewport'у.
 *   При скролле вниз граница не появляется никогда.
 * - [GradientTopBar] — прозрачный, рисуется поверх контента через z-order Box.
 *   Не добавляет свой фон → нет наложения двух градиентов → нет видимой полосы.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: Long,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userState by viewModel.userState.collectAsState()
    val stats     by viewModel.stats.collectAsState()

    LaunchedEffect(userId) { viewModel.initialize(userId) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Контент (скролл + градиент) ──────────────────────────────────────
        when (val state = userState) {
            is UiState.Loading -> LoadingWithBackground()
            is UiState.Error   -> ErrorWithBackground(message = state.message)
            is UiState.Success -> ProfileContent(
                user          = state.data,
                stats         = stats,
                onSaveProfile = { name, phone, birthDate ->
                    viewModel.saveProfile(userId, name, phone, birthDate)
                }
            )
            is UiState.Idle    -> Unit
        }

        // ── TopBar поверх всего, полностью прозрачный ────────────────────────
        // Рендерится последним в Box → поверх контента по z-order.
        // Не имеет собственного фона → не создаёт видимой полосы.
        GradientTopBar(
            title   = "Профиль",
            actions = {
                if (userState is UiState.Success) {
                    IconButton(onClick = { /* редактирование через inline-форму */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                    }
                }
            }
        )
    }
}

// ── Состояния Loading / Error с правильным фоном ─────────────────────────────

@Composable
private fun LoadingWithBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .scrollableGradientBackground(),
        contentAlignment = Alignment.Center
    ) {
        LoadingView(message = "Загрузка профиля...")
    }
}

@Composable
private fun ErrorWithBackground(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .scrollableGradientBackground(),
        contentAlignment = Alignment.Center
    ) {
        ErrorView(message = message, onRetry = null)
    }
}

// ── Основной контент профиля ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    user: UserModel,
    stats: ProfileStats,
    onSaveProfile: (name: String, phone: String, birthDate: Long?) -> Unit
) {
    val haptic         = LocalHapticFeedback.current
    var isEditing      by remember { mutableStateOf(false) }
    var editName       by remember(user.name)      { mutableStateOf(user.name) }
    var editPhone      by remember(user.phone)     { mutableStateOf(user.phone) }
    var editBirthDate  by remember(user.birthDate) { mutableStateOf(user.birthDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    val primary    = MaterialTheme.colorScheme.primary
    val isLoader   = user.role == UserRoleModel.LOADER
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    val age = user.birthDate?.let { ts ->
        val birth = Calendar.getInstance().apply { timeInMillis = ts }
        val now   = Calendar.getInstance()
        var y = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) y--
        y
    }

    val memberSince = remember(user.createdAt) {
        SimpleDateFormat("MMMM yyyy", Locale("ru")).format(Date(user.createdAt))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Порядок важен: фон рисуется ДО scroll, иначе он не тянется с контентом
            .scrollableGradientBackground()
            .verticalScroll(rememberScrollState())
            // Отступ под статус-бар + высота прозрачного TopBar (~56dp)
            .statusBarsPadding()
            .padding(top = 56.dp)
    ) {

        // ── Шапка профиля ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(primary.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Аватар-инициалы
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(primary.copy(0.3f), primary.copy(0.15f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = user.name.take(2).uppercase(),
                        fontSize   = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = primary
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(user.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(6.dp))

                // Роль + возраст
                Row(
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text     = if (isLoader) "Грузчик" else "Диспетчер",
                            fontSize = 13.sp,
                            color    = primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    if (age != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text     = "$age лет",
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text  = "В сервисе с $memberSince",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Звёздочки убраны по требованию — рейтинг отображается
                // в карточках статистики, дублировать его здесь избыточно
            }
        }

        // ── Статистика ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoader) {
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    value    = "${stats.completedOrders}",
                    label    = "заказов",
                    icon     = Icons.Default.CheckCircle,
                    color    = primary
                )
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    value    = "${stats.totalEarnings.toInt()} ₽",
                    label    = "заработано",
                    icon     = Icons.Default.Payments,
                    color    = Color(0xFF27AE60)
                )
            } else {
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    value    = "${stats.completedOrders}",
                    label    = "выполнено",
                    icon     = Icons.Default.CheckCircle,
                    color    = primary
                )
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    value    = "${stats.activeOrders}",
                    label    = "активных",
                    icon     = Icons.Default.WorkHistory,
                    color    = Color(0xFFE67E22)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(16.dp))

        // ── Личные данные ─────────────────────────────────────────────────────
        Text(
            text          = "Личные данные",
            fontSize      = 13.sp,
            fontWeight    = FontWeight.SemiBold,
            color         = primary,
            letterSpacing = 0.8.sp,
            modifier      = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        AnimatedVisibility(visible = !isEditing) {
            Column {
                ProfileInfoRow(Icons.Default.Person, "Имя",    user.name.ifBlank { "—" })
                ProfileInfoRow(Icons.Default.Phone,  "Телефон", user.phone.ifBlank { "Не указан" })
                ProfileInfoRow(
                    icon  = Icons.Default.Cake,
                    label = "Дата рождения",
                    value = user.birthDate
                        ?.let { dateFormat.format(Date(it)) + (age?.let { a -> " ($a лет)" } ?: "") }
                        ?: "Не указана"
                )
            }
        }

        AnimatedVisibility(visible = isEditing) {
            Column(
                modifier              = Modifier.padding(horizontal = 16.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value         = editName,
                    onValueChange = { editName = it },
                    label         = { Text("Имя") },
                    leadingIcon   = { Icon(Icons.Default.Person, null) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    shape         = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value         = editPhone,
                    onValueChange = { editPhone = it },
                    label         = { Text("Телефон") },
                    leadingIcon   = { Icon(Icons.Default.Phone, null) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape         = RoundedCornerShape(12.dp),
                    placeholder   = { Text("+7 999 000 00 00") }
                )
                OutlinedButton(
                    onClick  = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Cake, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = editBirthDate?.let { "ДР: " + dateFormat.format(Date(it)) }
                                     ?: "Указать дату рождения",
                        fontWeight = FontWeight.Normal
                    )
                }
                Button(
                    onClick  = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSaveProfile(editName, editPhone, editBirthDate)
                        isEditing = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("Сохранить", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        if (!isEditing) {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick  = { isEditing = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Редактировать профиль")
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showDatePicker) {
        BirthDatePickerDialog(
            initialDate    = editBirthDate,
            onDateSelected = { editBirthDate = it; showDatePicker = false },
            onDismiss      = { showDatePicker = false }
        )
    }
}

// ── Вспомогательные компоненты ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePickerDialog(
    initialDate: Long?,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            ?: Calendar.getInstance().apply { add(Calendar.YEAR, -25) }.timeInMillis,
        yearRange = 1950..2010
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton    = {
            TextButton(onClick = { state.selectedDateMillis?.let(onDateSelected) }) {
                Text("Выбрать")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    ) {
        DatePicker(
            state    = state,
            title    = {
                Text(
                    text     = "Дата рождения",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MiniStatCard(
    modifier: Modifier,
    value: String,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier  = modifier,
        elevation = CardDefaults.cardElevation(0.dp),
        shape     = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
