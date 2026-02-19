package com.loaderapp.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.Result
import com.loaderapp.data.model.User
import com.loaderapp.data.model.UserRole
import com.loaderapp.data.preferences.UserPreferences
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SessionDestination {
    /** DataStore ещё читается — навигировать нельзя */
    object Loading : SessionDestination()
    data class Dispatcher(val userId: Long) : SessionDestination()
    data class Loader(val userId: Long) : SessionDestination()
    object Auth : SessionDestination()

    /** Сессия определена (любой исход кроме Loading) */
    val isResolved: Boolean get() = this !is Loading
}

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
            _destination.value = when (val result = userRepository.getUserById(userId)) {
                is Result.Success -> when (result.data.role) {
                    UserRoleModel.DISPATCHER -> SessionDestination.Dispatcher(userId)
                    UserRoleModel.LOADER     -> SessionDestination.Loader(userId)
                }
                else -> SessionDestination.Auth
            }
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            userPreferences.clearCurrentUser()
            _destination.value = SessionDestination.Auth
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDarkTheme(enabled) }
    }

    fun onUserCreated(user: User, userId: Long) {
        viewModelScope.launch {
            userPreferences.setCurrentUserId(userId)
            _destination.value = when (user.role) {
                UserRole.DISPATCHER -> SessionDestination.Dispatcher(userId)
                UserRole.LOADER     -> SessionDestination.Loader(userId)
            }
        }
    }
}
