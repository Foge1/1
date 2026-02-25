package com.loaderapp.features.orders.domain.session

import com.loaderapp.features.orders.domain.Role
import kotlinx.coroutines.flow.Flow

data class OrdersUser(
    val id: String,
    val role: Role
)

interface OrdersUserSession {
    fun observeCurrentUser(): Flow<OrdersUser?>
    suspend fun getCurrentUserOrNull(): OrdersUser?
}
