package com.loaderapp.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.domain.model.UserRoleModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    isLoading: Boolean = false,
    error: String? = null,
    onLogin: (name: String, role: UserRoleModel) -> Unit
) {
    var name         by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<UserRoleModel?>(null) }
    var showError    by remember { mutableStateOf(false) }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
    }

    fun blockAlpha(start: Float, end: Float) =
        ((progress.value - start) / (end - start)).coerceIn(0f, 1f)
    fun blockOffset(start: Float, end: Float, from: Float = 22f) =
        from * (1f - ((progress.value - start) / (end - start)).coerceIn(0f, 1f))

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))

            // Логотип
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(80.dp)
                    .scale(0.72f + 0.28f * blockAlpha(0f, 0.42f))
                    .alpha(blockAlpha(0f, 0.38f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LocalShipping, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "ГрузчикиApp", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .alpha(blockAlpha(0.12f, 0.52f))
                    .offset(y = blockOffset(0.12f, 0.52f).dp)
            )
            Text(
                "Сервис поиска грузчиков",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .alpha(blockAlpha(0.18f, 0.58f))
                    .offset(y = blockOffset(0.18f, 0.58f).dp)
            )

            Spacer(Modifier.height(36.dp))

            // Поле имени
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; showError = false },
                label = { Text("Ваше имя") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                isError = showError && name.isBlank(),
                supportingText = if (showError && name.isBlank()) {
                    { Text("Введите имя") }
                } else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(blockAlpha(0.28f, 0.68f))
                    .offset(y = blockOffset(0.28f, 0.68f).dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(Modifier.height(24.dp))

            // Выбор роли
            Text(
                "Выберите роль",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(blockAlpha(0.38f, 0.72f))
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(blockAlpha(0.42f, 0.78f))
                    .offset(y = blockOffset(0.42f, 0.78f).dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RoleCard(
                    icon        = Icons.Default.LocalShipping,
                    title       = "Грузчик",
                    description = "Принимаю и выполняю заказы",
                    selected    = selectedRole == UserRoleModel.LOADER,
                    isError     = showError && selectedRole == null,
                    onClick     = { selectedRole = UserRoleModel.LOADER },
                    modifier    = Modifier.weight(1f)
                )
                RoleCard(
                    icon        = Icons.Default.Dashboard,
                    title       = "Диспетчер",
                    description = "Создаю и распределяю заказы",
                    selected    = selectedRole == UserRoleModel.DISPATCHER,
                    isError     = showError && selectedRole == null,
                    onClick     = { selectedRole = UserRoleModel.DISPATCHER },
                    modifier    = Modifier.weight(1f)
                )
            }

            if (showError && selectedRole == null) {
                Text(
                    "Выберите роль",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp)
                )
            }

            // Ошибка от ViewModel
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (name.isBlank() || selectedRole == null) {
                        showError = true
                    } else {
                        onLogin(name, selectedRole!!)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .alpha(blockAlpha(0.6f, 0.92f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Login, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Войти", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.03f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    val borderColor = when {
        selected -> MaterialTheme.colorScheme.primary
        isError  -> MaterialTheme.colorScheme.error
        else     -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor
        ),
        tonalElevation = if (selected) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}
