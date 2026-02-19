package com.loaderapp.presentation.session

import androidx.lifecycle.ViewModel
import com.loaderapp.data.dao.UserDao
import com.loaderapp.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Тонкий ViewModel для создания пользователя на экране Auth.
 * Работает с data.model.User напрямую — RoleSelectionScreen создаёт Entity,
 * нет смысла гонять через domain слой туда-обратно на этапе первичной регистрации.
 */
@HiltViewModel
class CreateUserViewModel @Inject constructor(
    private val userDao: UserDao
) : ViewModel() {

    suspend fun createUser(user: User): Long? {
        return try {
            userDao.insertUser(user)
        } catch (e: Exception) {
            null
        }
    }
}
