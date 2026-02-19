package com.loaderapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.presentation.settings.SettingsViewModel
import com.loaderapp.ui.components.AppScaffold
import com.loaderapp.ui.components.LocalTopBarHeightPx
import com.loaderapp.ui.main.LocalBottomNavHeight

@Composable
fun SettingsScreen(
    onSwitchRole: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val darkThemeEnabled     by viewModel.isDarkTheme.collectAsState()
    val notificationsEnabled by viewModel.isNotificationsEnabled.collectAsState()
    val soundEnabled         by viewModel.isSoundEnabled.collectAsState()
    val vibrationEnabled     by viewModel.isVibrationEnabled.collectAsState()

    AppScaffold(title = "Настройки") {
        val topBarHeightPx  = LocalTopBarHeightPx.current
        val density         = LocalDensity.current
        val topBarHeight    = with(density) { topBarHeightPx.toDp() }
        val bottomNavHeight = LocalBottomNavHeight.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = topBarHeight + 8.dp, bottom = bottomNavHeight + 16.dp)
        ) {
            SettingsSectionHeader("Внешний вид")

            SettingsToggleItem(
                icon            = Icons.Default.DarkMode,
                title           = "Тёмная тема",
                subtitle        = "Тёмное оформление интерфейса",
                checked         = darkThemeEnabled,
                onCheckedChange = { viewModel.setDarkTheme(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSectionHeader("Уведомления")

            SettingsToggleItem(
                icon            = Icons.Default.Notifications,
                title           = "Уведомления",
                subtitle        = "Получать уведомления о новых заказах",
                checked         = notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
            )
            SettingsToggleItem(
                icon            = Icons.Default.VolumeUp,
                title           = "Звук уведомлений",
                subtitle        = "Звук при новых заказах",
                checked         = soundEnabled && notificationsEnabled,
                enabled         = notificationsEnabled,
                onCheckedChange = { viewModel.setSoundEnabled(it) }
            )
            SettingsToggleItem(
                icon            = Icons.Default.Vibration,
                title           = "Вибрация",
                subtitle        = "Вибрация при новых уведомлениях",
                checked         = vibrationEnabled && notificationsEnabled,
                enabled         = notificationsEnabled,
                onCheckedChange = { viewModel.setVibrationEnabled(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSectionHeader("О приложении")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Версия приложения", fontWeight = FontWeight.Medium)
                    Text("GruzchikiApp 2.1", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsSectionHeader("Аккаунт")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSwitchRole() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SyncAlt, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Сменить роль", fontWeight = FontWeight.Medium)
                    Text("Переключиться между диспетчером и грузчиком",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text          = title,
        fontSize      = 12.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = MaterialTheme.colorScheme.primary,
        modifier      = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null,
                tint     = if (enabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
