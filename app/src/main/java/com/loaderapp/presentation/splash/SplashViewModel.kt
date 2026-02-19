package com.loaderapp.presentation.splash

import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.Result
import com.loaderapp.data.preferences.UserPreferences
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.usecase.user.GetUserByIdParams
import com.loaderapp.domain.usecase.user.GetUserByIdUseCase
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    data class Main(val userId: Long, val isDispatcher: Boolean) : SplashDestination()
    object Auth : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val getUserByIdUseCase: GetUserByIdUseCase
) : BaseViewModel() {

    private val _destination = MutableSharedFlow<SplashDestination>(replay = 0)
    val destination: SharedFlow<SplashDestination> = _destination.asSharedFlow()

    fun resolveStartDestination() {
        viewModelScope.launch {
            val userId = userPreferences.getCurrentUserId()
            if (userId != null) {
                val result = getUserByIdUseCase(GetUserByIdParams(userId))
                if (result is Result.Success) {
                    val isDispatcher = result.data.role == UserRoleModel.DISPATCHER
                    _destination.emit(SplashDestination.Main(userId, isDispatcher))
                } else {
                    _destination.emit(SplashDestination.Auth)
                }
            } else {
                _destination.emit(SplashDestination.Auth)
            }
        }
    }
}
