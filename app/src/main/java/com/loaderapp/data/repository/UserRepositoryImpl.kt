package com.loaderapp.data.repository

import com.loaderapp.core.common.Result
import com.loaderapp.data.datasource.local.UserLocalDataSource
import com.loaderapp.data.mapper.UserMapper
import com.loaderapp.data.model.UserRole
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Реализация UserRepository
 */
class UserRepositoryImpl
    @Inject
    constructor(
        private val localDataSource: UserLocalDataSource,
    ) : UserRepository {
        override fun getAllUsers(): Flow<List<UserModel>> =
            localDataSource
                .getAllUsers()
                .map { UserMapper.toDomainList(it) }

        override fun getLoaders(): Flow<List<UserModel>> =
            localDataSource
                .getUsersByRole(UserRole.LOADER)
                .map { UserMapper.toDomainList(it) }

        override fun getDispatchers(): Flow<List<UserModel>> =
            localDataSource
                .getUsersByRole(UserRole.DISPATCHER)
                .map { UserMapper.toDomainList(it) }

        override suspend fun getUserById(userId: Long): Result<UserModel> =
            runCatching {
                val user = localDataSource.getUserById(userId)
                if (user != null) {
                    Result.Success(UserMapper.toDomain(user))
                } else {
                    Result.Error("Пользователь не найден")
                }
            }.getOrElse { e ->
                Result.Error("Ошибка получения пользователя: ${e.message}", e)
            }

        override fun getUserByIdFlow(userId: Long): Flow<UserModel?> =
            localDataSource
                .getUserByIdFlow(userId)
                .map { it?.let { UserMapper.toDomain(it) } }

        override suspend fun getUserByNameAndRole(
            name: String,
            role: UserRoleModel,
        ): Result<UserModel?> =
            runCatching {
                val entityRole =
                    when (role) {
                        UserRoleModel.DISPATCHER -> UserRole.DISPATCHER
                        UserRoleModel.LOADER -> UserRole.LOADER
                    }
                val user = localDataSource.getUserByNameAndRole(name, entityRole)
                Result.Success(user?.let(UserMapper::toDomain))
            }.getOrElse { e ->
                Result.Error("Ошибка поиска пользователя: ${e.message}", e)
            }

        override suspend fun createUser(user: UserModel): Result<Long> =
            runCatching {
                val entity = UserMapper.toEntity(user)
                val id = localDataSource.insertUser(entity)
                Result.Success(id)
            }.getOrElse { e ->
                Result.Error("Ошибка создания пользователя: ${e.message}", e)
            }

        override suspend fun updateUser(user: UserModel): Result<Unit> =
            runCatching {
                val entity = UserMapper.toEntity(user)
                localDataSource.updateUser(entity)
                Result.Success(Unit)
            }.getOrElse { e ->
                Result.Error("Ошибка обновления пользователя: ${e.message}", e)
            }

        override suspend fun deleteUser(user: UserModel): Result<Unit> =
            runCatching {
                val entity = UserMapper.toEntity(user)
                localDataSource.deleteUser(entity)
                Result.Success(Unit)
            }.getOrElse { e ->
                Result.Error("Ошибка удаления пользователя: ${e.message}", e)
            }

        override suspend fun updateUserRating(
            userId: Long,
            rating: Double,
        ): Result<Unit> =
            runCatching {
                localDataSource.updateUserRating(userId, rating)
                Result.Success(Unit)
            }.getOrElse { e ->
                Result.Error("Ошибка обновления рейтинга: ${e.message}", e)
            }
    }
