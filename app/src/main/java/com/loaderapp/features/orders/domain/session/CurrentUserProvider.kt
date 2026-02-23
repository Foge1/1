package com.loaderapp.features.orders.domain.session

import com.loaderapp.features.orders.domain.Role
import kotlinx.coroutines.flow.Flow

data class CurrentUser(
    val id: String,
    val role: Role
)

interface CurrentUserProvider {
    fun observeCurrentUser(): Flow<CurrentUser>
    suspend fun getCurrentUser(): CurrentUser
}
