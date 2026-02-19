package com.loaderapp.presentation.rating

import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.Result
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.usecase.order.GetDispatcherStatsParams
import com.loaderapp.domain.usecase.order.GetDispatcherStatsUseCase
import com.loaderapp.domain.usecase.order.GetWorkerStatsParams
import com.loaderapp.domain.usecase.order.GetWorkerStatsUseCase
import com.loaderapp.domain.usecase.order.WorkerStats
import com.loaderapp.domain.usecase.order.DispatcherStats
import com.loaderapp.domain.usecase.user.GetUserByIdParams
import com.loaderapp.domain.usecase.user.GetUserByIdUseCase
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RatingUiData(
    val user: UserModel,
    val workerStats: WorkerStats? = null,
    val dispatcherStats: DispatcherStats? = null
)

@HiltViewModel
class RatingViewModel @Inject constructor(
    private val getUserByIdUseCase: GetUserByIdUseCase,
    private val getWorkerStatsUseCase: GetWorkerStatsUseCase,
    private val getDispatcherStatsUseCase: GetDispatcherStatsUseCase
) : BaseViewModel() {

    private val _state = MutableStateFlow<UiState<RatingUiData>>(UiState.Idle)
    val state: StateFlow<UiState<RatingUiData>> = _state.asStateFlow()

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

                if (isDispatcher) {
                    getDispatcherStatsUseCase(GetDispatcherStatsParams(userId))
                        .collect { stats ->
                            _state.setSuccess(RatingUiData(user = user, dispatcherStats = stats))
                        }
                } else {
                    getWorkerStatsUseCase(GetWorkerStatsParams(userId))
                        .collect { stats ->
                            _state.setSuccess(RatingUiData(user = user, workerStats = stats))
                        }
                }
            } catch (e: Exception) {
                _state.setError("Ошибка загрузки рейтинга")
            }
        }
    }
}
