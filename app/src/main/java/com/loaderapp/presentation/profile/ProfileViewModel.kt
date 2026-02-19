package com.loaderapp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.Result
import com.loaderapp.core.common.UiState
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileStats(
    val completedOrders: Int = 0,
    val totalEarnings: Double = 0.0,
    val averageRating: Float = 0f,
    val activeOrders: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<UiState<UserModel>>(UiState.Loading)
    val userState: StateFlow<UiState<UserModel>> = _userState.asStateFlow()

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats.asStateFlow()

    fun initialize(userId: Long) {
        viewModelScope.launch {
            // Загружаем пользователя как живой Flow — обновляется при изменениях
            userRepository.getUserByIdFlow(userId).collect { user ->
                _userState.value = if (user != null) UiState.Success(user)
                                   else UiState.Error("Пользователь не найден")
            }
        }
        viewModelScope.launch {
            // Статистика — три параллельных Flow, объединяем через combine
            combine(
                orderRepository.getCompletedOrdersCount(userId),
                orderRepository.getTotalEarnings(userId),
                orderRepository.getAverageRating(userId),
                orderRepository.getDispatcherActiveCount(userId)
            ) { completed, earnings, rating, active ->
                ProfileStats(
                    completedOrders = completed,
                    totalEarnings   = earnings ?: 0.0,
                    averageRating   = rating ?: 0f,
                    activeOrders    = active
                )
            }.collect { _stats.value = it }
        }
    }

    fun saveProfile(userId: Long, name: String, phone: String, birthDate: Long?) {
        viewModelScope.launch {
            val current = (_userState.value as? UiState.Success)?.data ?: return@launch
            userRepository.updateUser(
                current.copy(
                    name      = name.trim(),
                    phone     = phone.trim(),
                    birthDate = birthDate
                )
            )
        }
    }
}
