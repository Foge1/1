package com.loaderapp.presentation.profile

import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.Result
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.usecase.order.GetDispatcherStatsParams
import com.loaderapp.domain.usecase.order.GetDispatcherStatsUseCase
import com.loaderapp.domain.usecase.order.GetWorkerStatsParams
import com.loaderapp.domain.usecase.order.GetWorkerStatsUseCase
import com.loaderapp.domain.usecase.user.GetUserByIdParams
import com.loaderapp.domain.usecase.user.GetUserByIdUseCase
import com.loaderapp.domain.usecase.user.UpdateUserParams
import com.loaderapp.domain.usecase.user.UpdateUserUseCase
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiData(
    val user: UserModel,
    val completedCount: Int = 0,
    val totalEarnings: Double = 0.0,
    val averageRating: Float = 0f,
    val dispatcherCompletedCount: Int = 0,
    val dispatcherActiveCount: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val getWorkerStatsUseCase: GetWorkerStatsUseCase,
    private val getDispatcherStatsUseCase: GetDispatcherStatsUseCase,
    private val updateUserUseCase: UpdateUserUseCase
) : BaseViewModel() {

    private val _state = MutableStateFlow<UiState<ProfileUiData>>(UiState.Idle)
    val state: StateFlow<UiState<ProfileUiData>> = _state.asStateFlow()

    private var currentUser: UserModel? = null

    fun initialize(userId: Long, isDispatcher: Boolean) {
        viewModelScope.launch {
            _state.setLoading()
            try {
                val userResult = getUserByIdUseCase(GetUserByIdParams(userId))
                if (userResult is Result.Error) {
                    _state.setError(userResult.message)
                    return@launch
                }
                val user = (userResult as Result.Success).data
                currentUser = user

                if (isDispatcher) {
                    getDispatcherStatsUseCase(GetDispatcherStatsParams(userId))
                        .collect { stats ->
                            _state.setSuccess(
                                ProfileUiData(
                                    user = user,
                                    dispatcherCompletedCount = stats.completedOrders,
                                    dispatcherActiveCount = stats.activeOrders
                                )
                            )
                        }
                } else {
                    getWorkerStatsUseCase(GetWorkerStatsParams(userId))
                        .collect { stats ->
                            _state.setSuccess(
                                ProfileUiData(
                                    user = user,
                                    completedCount = stats.completedOrders,
                                    totalEarnings = stats.totalEarnings,
                                    averageRating = stats.averageRating
                                )
                            )
                        }
                }
            } catch (e: Exception) {
                _state.setError("Ошибка загрузки профиля")
            }
        }
    }

    fun saveProfile(name: String, phone: String, birthDate: Long?) {
        val user = currentUser ?: return
        launchSafe {
            val updated = user.copy(name = name, phone = phone, birthDate = birthDate)
            val result = updateUserUseCase(UpdateUserParams(updated))
            if (result is Result.Success) {
                currentUser = updated
                showSnackbar("Профиль сохранён")
            } else if (result is Result.Error) {
                showSnackbar(result.message)
            }
        }
    }
}
