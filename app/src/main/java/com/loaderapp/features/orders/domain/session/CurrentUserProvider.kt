package com.loaderapp.features.orders.domain.session

import com.loaderapp.features.orders.domain.Role

data class CurrentUser(
    val id: String,
    val role: Role
)

interface CurrentUserProvider {
    suspend fun getCurrentUser(): CurrentUser
}
