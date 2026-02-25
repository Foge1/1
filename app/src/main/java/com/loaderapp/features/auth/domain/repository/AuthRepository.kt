package com.loaderapp.features.auth.domain.repository

import com.loaderapp.core.common.AppResult
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.features.auth.domain.model.SessionState
import com.loaderapp.features.auth.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun restoreSession(): AppResult<Unit>

    suspend fun login(name: String, role: UserRoleModel): AppResult<User>

    suspend fun logout(): AppResult<Unit>

    fun observeSession(): Flow<SessionState>
}
