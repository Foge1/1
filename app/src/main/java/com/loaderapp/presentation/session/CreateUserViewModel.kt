package com.loaderapp.presentation.session

import androidx.lifecycle.ViewModel
import com.loaderapp.core.common.Result
import com.loaderapp.data.mapper.UserMapper
import com.loaderapp.data.model.User
import com.loaderapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel для создания пользователя на экране Auth.
 *
 * Использует [UserRepository] (domain-интерфейс), а не DAO напрямую —
 * слои соблюдены, маппинг Entity→Model скрыт в репозитории.
 */
@HiltViewModel
class CreateUserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    /**
     * Создаёт пользователя и возвращает его ID.
     * [user] — data.model.User из RoleSelectionScreen;
     * конвертируется в domain-модель через UserMapper.
     */
    suspend fun createUser(user: User): Long? {
        val domainModel = UserMapper.toDomain(user)
        return when (val result = userRepository.createUser(domainModel)) {
            is Result.Success -> result.data
            else -> null
        }
    }
}
