package com.loaderapp.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.presentation.settings.SettingsViewModel

/**
 * Вкладка «Настройки».
 * Состояние и логика — через SettingsViewModel → UserPreferences.
 * Нет прямого доступа к LocalContext / LoaderApplication.
 */
@Composable
fun SettingsScreen(
    onSwitchRole: () -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit
) {
    val viewModel: SettingsViewModel = hiltViewModel()

    SettingsScreen(
        onMenuClick = {},
        onBackClick = {},
        onDarkThemeChanged = { enabled ->
            viewModel.setDarkTheme(enabled)
            onDarkThemeChanged(enabled)
        },
        onSwitchRole = {
            viewModel.clearCurrentUser { onSwitchRole() }
        }
    )
}
