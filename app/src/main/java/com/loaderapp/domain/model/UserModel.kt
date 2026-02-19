package com.loaderapp.domain.model

/**
 * Domain модель пользователя
 * Чистая бизнес-модель без зависимостей от фреймворка
 */
data class UserModel(
    val id: Long,
    val name: String,
    val phone: String,
    val role: UserRoleModel,
    val rating: Double,
    val birthDate: Long?,
    val avatarInitials: String,
    val createdAt: Long
)

enum class UserRoleModel {
    DISPATCHER,
    LOADER
}
