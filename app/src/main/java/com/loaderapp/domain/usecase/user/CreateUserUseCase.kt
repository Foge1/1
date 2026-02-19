package com.loaderapp.domain.usecase.user

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.domain.usecase.base.UseCase
import javax.inject.Inject

/**
 * Параметры для создания пользователя
 */
data class CreateUserParams(val user: UserModel)

/**
 * UseCase: Создать нового пользователя
 * 
 * Бизнес-правила:
 * - Имя не пустое (минимум 2 символа)
 * - Телефон в формате +7XXXXXXXXXX или валидный международный
 * - Автоматическое создание инициалов для аватара
 */
class CreateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) : UseCase<CreateUserParams, Long>() {
    
    override suspend fun execute(params: CreateUserParams): Result<Long> {
        val user = params.user
        
        // Валидация имени
        if (user.name.isBlank() || user.name.length < 2) {
            return Result.Error("Имя должно содержать минимум 2 символа")
        }
        
        // Валидация телефона
        if (!isValidPhone(user.phone)) {
            return Result.Error("Некорректный формат телефона")
        }
        
        // Генерация инициалов, если не заданы
        val userWithInitials = if (user.avatarInitials.isBlank()) {
            user.copy(avatarInitials = generateInitials(user.name))
        } else {
            user
        }
        
        // Создание пользователя
        return userRepository.createUser(userWithInitials)
    }
    
    /**
     * Валидация телефона
     */
    private fun isValidPhone(phone: String): Boolean {
        // Упрощённая валидация: должен содержать минимум 10 цифр
        val digitsOnly = phone.filter { it.isDigit() }
        return digitsOnly.length >= 10
    }
    
    /**
     * Генерация инициалов из имени
     */
    private fun generateInitials(name: String): String {
        val parts = name.trim().split(" ")
        return when {
            parts.size >= 2 -> {
                // Имя Фамилия -> ИФ
                "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            }
            parts.size == 1 && parts[0].length >= 2 -> {
                // Имя -> первые 2 буквы
                parts[0].take(2).uppercase()
            }
            else -> {
                parts[0].first().uppercase()
            }
        }
    }
}
