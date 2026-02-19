package com.loaderapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Владеет всеми настройками пользователя из DataStore.
 * Единственный источник истины для SettingsScreen и тёмной темы.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = userPreferences.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isNotificationsEnabled: StateFlow<Boolean> = userPreferences.isNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isSoundEnabled: StateFlow<Boolean> = userPreferences.isSoundEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isVibrationEnabled: StateFlow<Boolean> = userPreferences.isVibrationEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDarkTheme(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setNotificationsEnabled(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setVibrationEnabled(enabled) }
    }
}
