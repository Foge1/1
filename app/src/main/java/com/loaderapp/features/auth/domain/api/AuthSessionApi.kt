package com.loaderapp.features.auth.domain.api

import com.loaderapp.features.auth.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthSessionApi {
    fun observeCurrentUser(): Flow<User?>

    suspend fun getCurrentUserOrNull(): User?
}

