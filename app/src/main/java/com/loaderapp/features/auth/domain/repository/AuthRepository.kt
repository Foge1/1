package com.loaderapp.features.auth.domain.repository

import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel

/**
 * Контракт репозитория аутентификации.
 * TODO: Реализовать AuthRepositoryImpl когда будет добавлена серверная авторизация.
 */
interface AuthRepository {
    /** Войти по номеру телефона + PIN */
    suspend fun login(phone: String, pin: String): Result<UserModel>

    /** Зарегистрироваться */
    suspend fun register(name: String, phone: String, pin: String, role: UserRoleModel): Result<UserModel>

    /** Выйти из системы */
    suspend fun logout()

    /** Текущий авторизованный пользователь или null */
    suspend fun getCurrentUser(): UserModel?

    /** Сохранить сессию пользователя */
    suspend fun saveSession(userId: Long)
}
