package com.loaderapp.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.core.common.UiState
import com.loaderapp.core.ui.theme.AppColors
import com.loaderapp.core.ui.theme.AppSpacing
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.presentation.profile.ProfileStats
import com.loaderapp.presentation.profile.ProfileViewModel
import com.loaderapp.ui.common.asString
import com.loaderapp.ui.components.AppScaffold
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView
import com.loaderapp.ui.components.LocalTopBarHeightPx
import com.loaderapp.ui.components.scrollableGradientBackground
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    userId: Long,
    onNavigateToRating: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val userState by viewModel.userState.collectAsState()
    val stats by viewModel.stats.collectAsState()

    LaunchedEffect(userId) { viewModel.initialize(userId) }

    AppScaffold(title = "Профиль") {
        val density = LocalDensity.current
        val topBarHeightPx = LocalTopBarHeightPx.current
        val topBarDp = with(density) { topBarHeightPx.toDp() }

        when (val state = userState) {
            is UiState.Loading -> LoadingView()
            is UiState.Error -> ErrorView(message = state.message.asString(), onRetry = null)
            is UiState.Success -> {
                ProfileContent(
                    user = state.data,
                    stats = stats,
                    topPadding = topBarDp,
                    onSaveProfile = viewModel::saveProfile,
                    onNavigateToRating = onNavigateToRating,
                )
            }
            is UiState.Idle -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    user: UserModel,
    stats: ProfileStats,
    topPadding: Dp,
    onSaveProfile: (name: String, phone: String, birthDate: Long?) -> Unit,
    onNavigateToRating: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember(user.name) { mutableStateOf(user.name) }
    var editPhone by remember(user.phone) { mutableStateOf(user.phone) }
    var editBirthDate by remember(user.birthDate) { mutableStateOf(user.birthDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    val age = remember(user.birthDate) { calculateAge(user.birthDate) }
    val memberSince = remember(user.createdAt) { SimpleDateFormat("MMMM yyyy", Locale("ru")).format(Date(user.createdAt)) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .scrollableGradientBackground()
                .padding(top = topPadding, start = AppSpacing.lg, end = AppSpacing.lg, bottom = AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
        AvatarCard(
            user = user,
            age = age,
            memberSince = memberSince,
        )

        StatsGrid(
            userRole = user.role,
            stats = stats,
            onNavigateToRating = onNavigateToRating,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

        val profileDataSectionState =
            ProfileDataSectionState(
                name = user.name.ifBlank { "—" },
                phone = user.phone.ifBlank { "Не указан" },
                birthDate =
                    user.birthDate?.let {
                        "${dateFormat.format(Date(it))}${age?.let { years -> " ($years лет)" } ?: ""}"
                    } ?: "Не указана",
                isEditing = isEditing,
                editName = editName,
                editPhone = editPhone,
                editBirthDate = editBirthDate,
            )
        val profileDataSectionActions =
            ProfileDataSectionActions(
                onNameChange = { editName = it },
                onPhoneChange = { editPhone = it },
                onShowDatePicker = { showDatePicker = true },
                onSave = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSaveProfile(editName, editPhone, editBirthDate)
                    isEditing = false
                },
                onEditClick = { isEditing = true },
            )

        ProfileDataSection(
            state = profileDataSectionState,
            actions = profileDataSectionActions,
            dateFormat = dateFormat,
        )
    }

    if (showDatePicker) {
        BirthDatePickerDialog(
            initialDate = editBirthDate,
            onDateSelected = {
                editBirthDate = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun AvatarCard(
    user: UserModel,
    age: Int?,
    memberSince: String,
) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(84.dp)
                        .background(
                            brush = Brush.radialGradient(listOf(primary.copy(alpha = 0.3f), primary.copy(alpha = 0.1f))),
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = user.name.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = primary,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                RoleChip(role = user.role)
                if (age != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "$age лет",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
                        )
                    }
                }
            }

            Text(
                text = "В сервисе с $memberSince",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RoleChip(role: UserRoleModel) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    ) {
        Text(
            text = if (role == UserRoleModel.LOADER) "Грузчик" else "Диспетчер",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs),
        )
    }
}

@Composable
private fun StatsGrid(
    userRole: UserRoleModel,
    stats: ProfileStats,
    onNavigateToRating: () -> Unit,
) {
    val gridItems =
        listOf(
            StatItem("Выполнено", stats.completedOrders.toString(), Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary),
            StatItem(
                "Рейтинг",
                if (stats.averageRating > 0f) "%.1f".format(stats.averageRating) else "—",
                Icons.Default.Star,
                AppColors.Accent,
                onClick = onNavigateToRating,
            ),
            StatItem(
                "Активных",
                if (userRole == UserRoleModel.DISPATCHER) stats.activeOrders.toString() else "—",
                Icons.Default.WorkHistory,
                AppColors.StatusCanceledFg,
            ),
            StatItem(
                "Доход",
                if (userRole == UserRoleModel.LOADER) "${stats.totalEarnings.toInt()} ₽" else "—",
                Icons.Default.Payments,
                AppColors.StatusStaffingFg,
            ),
        )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxWidth().height(188.dp),
        contentPadding = PaddingValues(0.dp),
    ) {
        items(gridItems) { item ->
            StatCard(item = item)
        }
    }
}

private data class StatItem(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: (() -> Unit)? = null,
)

@Composable
private fun StatCard(item: StatItem) {
    val cardModifier =
        item.onClick?.let { onClick ->
            Modifier.selectable(
                selected = false,
                onClick = onClick,
                role = Role.Button,
            )
        } ?: Modifier

    Card(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(AppSpacing.md),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(imageVector = item.icon, contentDescription = null, tint = item.tint)
            Column {
                Text(text = item.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = item.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePickerDialog(
    initialDate: Long?,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = initialDate ?: Calendar.getInstance().apply { add(Calendar.YEAR, -25) }.timeInMillis,
            yearRange = 1950..2010,
        )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let(onDateSelected) }) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    ) {
        DatePicker(
            state = state,
            title = {
                Text(
                    text = "Дата рождения",
                    modifier = Modifier.padding(start = AppSpacing.xxl, top = AppSpacing.lg),
                )
            },
        )
    }
}

private data class ProfileDataSectionState(
    val name: String,
    val phone: String,
    val birthDate: String,
    val isEditing: Boolean,
    val editName: String,
    val editPhone: String,
    val editBirthDate: Long?,
)

private data class ProfileDataSectionActions(
    val onNameChange: (String) -> Unit,
    val onPhoneChange: (String) -> Unit,
    val onShowDatePicker: () -> Unit,
    val onSave: () -> Unit,
    val onEditClick: () -> Unit,
)

@Composable
private fun ProfileDataSection(
    state: ProfileDataSectionState,
    actions: ProfileDataSectionActions,
    dateFormat: SimpleDateFormat,
) {
    Text(
        text = "Личные данные",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )

    AnimatedVisibility(visible = !state.isEditing) {
        Column {
            ProfileInfoRow(Icons.Default.Person, "Имя", state.name)
            ProfileInfoRow(Icons.Default.Phone, "Телефон", state.phone)
            ProfileInfoRow(
                Icons.Default.Cake,
                "Дата рождения",
                state.birthDate,
            )
        }
    }

    AnimatedVisibility(visible = state.isEditing) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            OutlinedTextField(
                value = state.editName,
                onValueChange = actions.onNameChange,
                label = { Text("Имя") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            OutlinedTextField(
                value = state.editPhone,
                onValueChange = actions.onPhoneChange,
                label = { Text("Телефон") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            OutlinedButton(
                onClick = actions.onShowDatePicker,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Cake, contentDescription = null)
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                Text(state.editBirthDate?.let { "ДР: ${dateFormat.format(Date(it))}" } ?: "Указать дату рождения")
            }
            Button(onClick = actions.onSave, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Сохранить")
            }
        }
    }

    if (!state.isEditing) {
        TextButton(onClick = actions.onEditClick) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(modifier = Modifier.width(AppSpacing.xs))
            Text("Редактировать профиль")
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(AppSpacing.md))
        Column {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun calculateAge(birthDate: Long?): Int? =
    birthDate?.let { ts ->
        val birth = Calendar.getInstance().apply { timeInMillis = ts }
        val now = Calendar.getInstance()
        var years = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
            years--
        }
        years
    }
