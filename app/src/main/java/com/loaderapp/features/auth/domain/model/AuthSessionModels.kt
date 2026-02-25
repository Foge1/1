package com.loaderapp.features.auth.domain.model

import com.loaderapp.core.common.AppError
import com.loaderapp.domain.model.UserRoleModel

data class User(
    val id: Long,
    val name: String,
    val role: UserRoleModel,
    val phone: String = "",
    val rating: Double = 5.0,
    val birthDate: Long? = null,
    val avatarInitials: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

sealed interface SessionState {
    data object Unauthenticated : SessionState
    data object Authenticating : SessionState
    data class Authenticated(val user: User) : SessionState
    data class Error(val error: AppError) : SessionState
}
