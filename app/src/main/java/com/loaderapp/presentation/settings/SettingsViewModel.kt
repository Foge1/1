package com.loaderapp.presentation.settings

import androidx.lifecycle.viewModelScope
import com.loaderapp.data.preferences.UserPreferences
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : BaseViewModel() {

    val isDarkTheme: StateFlow<Boolean> = userPreferences.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isNotificationsEnabled: StateFlow<Boolean> = userPreferences.isNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val isSoundEnabled: StateFlow<Boolean> = userPreferences.isSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDarkTheme(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setNotificationsEnabled(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setSoundEnabled(enabled) }
    }

    fun clearCurrentUser(onDone: () -> Unit) {
        viewModelScope.launch {
            userPreferences.clearCurrentUser()
            onDone()
        }
    }
}
