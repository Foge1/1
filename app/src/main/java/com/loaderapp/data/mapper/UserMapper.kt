package com.loaderapp.data.mapper

import com.loaderapp.data.model.User
import com.loaderapp.data.model.UserRole
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel

/**
 * Mapper для конвертации User между data и domain слоями
 */
object UserMapper {
    
    /**
     * Конвертация Data Entity -> Domain Model
     */
    fun toDomain(entity: User): UserModel {
        return UserModel(
            id = entity.id,
            name = entity.name,
            phone = entity.phone,
            role = entity.role.toDomain(),
            rating = entity.rating,
            birthDate = entity.birthDate,
            avatarInitials = entity.avatarInitials,
            createdAt = entity.createdAt
        )
    }
    
    /**
     * Конвертация Domain Model -> Data Entity
     */
    fun toEntity(model: UserModel): User {
        return User(
            id = model.id,
            name = model.name,
            phone = model.phone,
            role = model.role.toEntity(),
            rating = model.rating,
            birthDate = model.birthDate,
            avatarInitials = model.avatarInitials,
            createdAt = model.createdAt
        )
    }
    
    /**
     * Конвертация списка Entity -> Domain
     */
    fun toDomainList(entities: List<User>): List<UserModel> {
        return entities.map { toDomain(it) }
    }
    
    /**
     * Конвертация списка Domain -> Entity
     */
    fun toEntityList(models: List<UserModel>): List<User> {
        return models.map { toEntity(it) }
    }
}

/**
 * Extension функции для UserRole
 */
private fun UserRole.toDomain(): UserRoleModel {
    return when (this) {
        UserRole.DISPATCHER -> UserRoleModel.DISPATCHER
        UserRole.LOADER -> UserRoleModel.LOADER
    }
}

private fun UserRoleModel.toEntity(): UserRole {
    return when (this) {
        UserRoleModel.DISPATCHER -> UserRole.DISPATCHER
        UserRoleModel.LOADER -> UserRole.LOADER
    }
}
