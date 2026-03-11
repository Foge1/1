package com.loaderapp.presentation.dispatcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.usecase.order.GetDispatcherStatsParams
import com.loaderapp.domain.usecase.order.GetDispatcherStatsUseCase
import com.loaderapp.features.auth.domain.api.AuthSessionApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DispatcherStatsViewModel
    @Inject
    constructor(
        authSessionApi: AuthSessionApi,
        private val getDispatcherStatsUseCase: GetDispatcherStatsUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DispatcherStatsUiState())
        val uiState: StateFlow<DispatcherStatsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                authSessionApi
                    .observeCurrentUser()
                    .flatMapLatest { user ->
                        if (user == null || user.role != UserRoleModel.DISPATCHER) {
                            flowOf(null)
                        } else {
                            getDispatcherStatsUseCase(GetDispatcherStatsParams(user.id))
                        }
                    }
                    .collect { stats ->
                        _uiState.update {
                            it.copy(
                                active = stats?.activeOrders ?: 0,
                                completed = stats?.completedOrders ?: 0,
                            )
                        }
                    }
            }
        }
    }

data class DispatcherStatsUiState(
    val active: Int = 0,
    val completed: Int = 0,
)
