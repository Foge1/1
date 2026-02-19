package com.loaderapp.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.data.model.User
import com.loaderapp.data.model.UserRole
import com.loaderapp.data.preferences.UserPreferences
import com.loaderapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SessionDestination {
    object Loading : SessionDestination()
    data class Dispatcher(val userId: Long) : SessionDestination()
    data class Loader(val userId: Long) : SessionDestination()
    object Auth : SessionDestination()
}

/**
 * Отвечает за определение стартового маршрута на основе сохранённой сессии.
 * Инжектируется через Hilt — не требует ручного создания в Application.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SessionDestination>(SessionDestination.Loading)
    val destination: StateFlow<SessionDestination> = _destination.asStateFlow()

    init {
        resolveStartDestination()
    }

    private fun resolveStartDestination() {
        viewModelScope.launch {
            val userId = userPreferences.getCurrentUserId()
            if (userId == null) {
                _destination.value = SessionDestination.Auth
                return@launch
            }

            val result = userRepository.getUserById(userId)
            val user = (result as? com.loaderapp.core.common.Result.Success)?.data

            _destination.value = when (user?.role) {
                com.loaderapp.domain.model.UserRoleModel.DISPATCHER -> SessionDestination.Dispatcher(userId)
                com.loaderapp.domain.model.UserRoleModel.LOADER -> SessionDestination.Loader(userId)
                null -> SessionDestination.Auth
            }
        }
    }

    fun saveSession(userId: Long) {
        viewModelScope.launch {
            userPreferences.setCurrentUserId(userId)
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            userPreferences.clearCurrentUser()
            _destination.value = SessionDestination.Auth
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setDarkTheme(enabled)
        }
    }

    /**
     * Вызывается после успешного создания пользователя на экране Auth.
     * Принимает data.model.User (Entity) — RouteSelectionScreen работает с ним напрямую.
     */
    fun onUserCreated(user: User, userId: Long) {
        viewModelScope.launch {
            userPreferences.setCurrentUserId(userId)
            _destination.value = when (user.role) {
                UserRole.DISPATCHER -> SessionDestination.Dispatcher(userId)
                UserRole.LOADER -> SessionDestination.Loader(userId)
            }
        }
    }
}
