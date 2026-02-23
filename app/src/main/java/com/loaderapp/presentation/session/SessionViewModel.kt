package com.loaderapp.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.Result
import com.loaderapp.data.preferences.UserPreferences
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.usecase.user.CreateUserParams
import com.loaderapp.domain.usecase.user.CreateUserUseCase
import com.loaderapp.domain.usecase.user.GetUserByIdParams
import com.loaderapp.domain.usecase.user.GetUserByIdUseCase
import com.loaderapp.domain.usecase.user.GetUserByNameAndRoleParams
import com.loaderapp.domain.usecase.user.GetUserByNameAndRoleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SessionDestination {
    object Loading : SessionDestination()
    object Auth    : SessionDestination()
    object Main    : SessionDestination()

    val isResolved: Boolean get() = this !is Loading
}

data class SessionState(
    val user: UserModel?   = null,
    val isLoading: Boolean = false,
    val error: String?     = null
)

/**
 * ViewModel managing authentication session lifecycle.
 *
 * Uses [CreateUserUseCase] and [GetUserByIdUseCase] — no direct Repository access.
 * This keeps the ViewModel inside Clean Architecture boundaries and ensures
 * business rules (name validation, initials generation) are always applied.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val createUserUseCase: CreateUserUseCase,
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val getUserByNameAndRoleUseCase: GetUserByNameAndRoleUseCase
) : ViewModel() {

    private val _destination = MutableStateFlow<SessionDestination>(SessionDestination.Loading)
    val destination: StateFlow<SessionDestination> = _destination.asStateFlow()

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    val currentUser: StateFlow<UserModel?> = sessionState
        .map { it.user }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        resolveSession()
    }

    private fun resolveSession() {
        viewModelScope.launch {
            val userId = userPreferences.getCurrentUserId()
            if (userId == null) {
                _destination.value = SessionDestination.Auth
                return@launch
            }
            when (val result = getUserByIdUseCase(GetUserByIdParams(userId))) {
                is Result.Success -> {
                    _sessionState.value = SessionState(user = result.data)
                    _destination.value  = SessionDestination.Main
                }
                else -> {
                    userPreferences.clearCurrentUser()
                    _destination.value = SessionDestination.Auth
                }
            }
        }
    }

    fun login(name: String, role: UserRoleModel) {
        viewModelScope.launch {
            _sessionState.value = SessionState(isLoading = true)

            val normalizedName = name.trim()

            when (val existingUserResult =
                getUserByNameAndRoleUseCase(GetUserByNameAndRoleParams(normalizedName, role))) {
                is Result.Success -> {
                    val existingUser = existingUserResult.data
                    if (existingUser != null) {
                        userPreferences.setCurrentUserId(existingUser.id)
                        _sessionState.value = SessionState(user = existingUser)
                        _destination.value = SessionDestination.Main
                        return@launch
                    }
                }
                is Result.Error -> {
                    _sessionState.value = SessionState(error = existingUserResult.message)
                    return@launch
                }
                is Result.Loading -> Unit
            }

            val domainUser = UserModel(
                id             = 0,
                name           = normalizedName,
                phone          = "",
                role           = role,
                rating         = 5.0,
                birthDate      = null,
                avatarInitials = "",   // CreateUserUseCase will generate this
                createdAt      = System.currentTimeMillis()
            )

            when (val createResult = createUserUseCase(CreateUserParams(domainUser))) {
                is Result.Success -> {
                    val userId = createResult.data
                    userPreferences.setCurrentUserId(userId)
                    when (val userResult = getUserByIdUseCase(GetUserByIdParams(userId))) {
                        is Result.Success -> {
                            _sessionState.value = SessionState(user = userResult.data)
                            _destination.value  = SessionDestination.Main
                        }
                        else -> _sessionState.value = SessionState(error = "Ошибка загрузки профиля")
                    }
                }
                is Result.Error -> _sessionState.value = SessionState(error = createResult.message)
                else -> Unit
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearCurrentUser()
            _sessionState.value = SessionState()
            _destination.value  = SessionDestination.Auth
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDarkTheme(enabled) }
    }
}
