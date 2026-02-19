package com.loaderapp.domain.repository

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с пользователями
 */
interface UserRepository {
    
    /**
     * Получить всех пользователей
     */
    fun getAllUsers(): Flow<List<UserModel>>
    
    /**
     * Получить грузчиков
     */
    fun getLoaders(): Flow<List<UserModel>>
    
    /**
     * Получить диспетчеров
     */
    fun getDispatchers(): Flow<List<UserModel>>
    
    /**
     * Получить пользователя по ID
     */
    suspend fun getUserById(userId: Long): Result<UserModel>
    
    /**
     * Получить пользователя по ID как Flow
     */
    fun getUserByIdFlow(userId: Long): Flow<UserModel?>
    
    /**
     * Создать пользователя
     */
    suspend fun createUser(user: UserModel): Result<Long>
    
    /**
     * Обновить пользователя
     */
    suspend fun updateUser(user: UserModel): Result<Unit>
    
    /**
     * Удалить пользователя
     */
    suspend fun deleteUser(user: UserModel): Result<Unit>
    
    /**
     * Обновить рейтинг пользователя
     */
    suspend fun updateUserRating(userId: Long, rating: Double): Result<Unit>
}
