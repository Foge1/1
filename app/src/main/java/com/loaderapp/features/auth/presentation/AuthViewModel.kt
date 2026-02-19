package com.loaderapp.features.auth.presentation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel для экранов аутентификации.
 * TODO: Заменить RoleSelectionScreen на полноценный Auth flow с этим ViewModel.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    // TODO: inject LoginUseCase, RegisterUseCase когда будет AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    // TODO: fun login(phone: String, pin: String)
    // TODO: fun register(name: String, phone: String, pin: String, role: UserRoleModel)
    // TODO: fun logout()
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val userId: Long) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
