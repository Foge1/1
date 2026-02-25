package com.loaderapp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.UiState
import com.loaderapp.core.common.toAppError
import com.loaderapp.core.common.UiText
import com.loaderapp.presentation.common.toUiText
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.usecase.order.GetDispatcherStatsParams
import com.loaderapp.domain.usecase.order.GetDispatcherStatsUseCase
import com.loaderapp.domain.usecase.order.GetWorkerStatsParams
import com.loaderapp.domain.usecase.order.GetWorkerStatsUseCase
import com.loaderapp.domain.usecase.user.GetUserByIdFlowParams
import com.loaderapp.domain.usecase.user.GetUserByIdFlowUseCase
import com.loaderapp.domain.usecase.user.UpdateUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileStats(
    val completedOrders: Int = 0,
    val totalEarnings: Double = 0.0,
    val averageRating: Float = 0f,
    val activeOrders: Int = 0
)

/**
 * ViewModel экрана профиля.
 *
 * Зависимости исключительно через UseCases:
 *  - [GetUserByIdFlowUseCase]    — реактивная подписка на пользователя
 *  - [GetWorkerStatsUseCase]     — статистика грузчика
 *  - [GetDispatcherStatsUseCase] — статистика диспетчера
 *  - [UpdateUserUseCase]         — сохранение профиля
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserByIdFlowUseCase: GetUserByIdFlowUseCase,
    private val getWorkerStatsUseCase: GetWorkerStatsUseCase,
    private val getDispatcherStatsUseCase: GetDispatcherStatsUseCase,
    private val updateUserUseCase: UpdateUserUseCase
) : ViewModel() {

    private val _userState = MutableStateFlow<UiState<UserModel>>(UiState.Loading)
    val userState: StateFlow<UiState<UserModel>> = _userState.asStateFlow()

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats.asStateFlow()

    fun initialize(userId: Long) {
        viewModelScope.launch {
            getUserByIdFlowUseCase(GetUserByIdFlowParams(userId))
                .collect { user ->
                    if (user != null) {
                        _userState.value = UiState.Success(user)
                        loadStats(userId, user.role)
                    } else {
                        _userState.value = UiState.Error(UiText.Dynamic("Пользователь не найден"))
                    }
                }
        }
    }

    private fun loadStats(userId: Long, role: UserRoleModel) {
        viewModelScope.launch {
            when (role) {
                UserRoleModel.LOADER -> {
                    getWorkerStatsUseCase(GetWorkerStatsParams(userId))
                        .collect { workerStats ->
                            _stats.value = ProfileStats(
                                completedOrders = workerStats.completedOrders,
                                totalEarnings   = workerStats.totalEarnings,
                                averageRating   = workerStats.averageRating
                            )
                        }
                }
                UserRoleModel.DISPATCHER -> {
                    getDispatcherStatsUseCase(GetDispatcherStatsParams(userId))
                        .collect { dispatcherStats ->
                            _stats.value = ProfileStats(
                                completedOrders = dispatcherStats.completedOrders,
                                activeOrders    = dispatcherStats.activeOrders
                            )
                        }
                }
            }
        }
    }

    fun saveProfile(userId: Long, name: String, phone: String, birthDate: Long?) {
        val current = (_userState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            updateUserUseCase(
                current.copy(
                    name      = name.trim(),
                    phone     = phone.trim(),
                    birthDate = birthDate
                )
            )
        }
    }
}
