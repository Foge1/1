package com.loaderapp.ui.settings

/**
 * Обёртка вкладки «Настройки».
 * SettingsScreen самостоятельно получает данные через LocalContext → LoaderApplication,
 * поэтому здесь достаточно прокинуть колбэки.
 */
@androidx.compose.runtime.Composable
fun SettingsScreen(
    onSwitchRole: () -> Unit,
    onDarkThemeChanged: (Boolean) -> Unit
) {
    SettingsScreen(
        onMenuClick = {},
        onBackClick = {},
        onDarkThemeChanged = onDarkThemeChanged,
        onSwitchRole = onSwitchRole
    )
}
