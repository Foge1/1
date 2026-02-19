package com.loaderapp.data.datasource.local

import com.loaderapp.data.dao.UserDao
import com.loaderapp.data.model.User
import com.loaderapp.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * LocalDataSource для работы с пользователями через Room
 */
class UserLocalDataSource @Inject constructor(
    private val userDao: UserDao
) {
    
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()
    
    fun getUsersByRole(role: UserRole): Flow<List<User>> = 
        userDao.getUsersByRole(role)
    
    suspend fun getUserById(userId: Long): User? = 
        userDao.getUserById(userId)
    
    fun getUserByIdFlow(userId: Long): Flow<User?> = 
        userDao.getUserByIdFlow(userId)
    
    suspend fun insertUser(user: User): Long = 
        userDao.insertUser(user)
    
    suspend fun updateUser(user: User) = 
        userDao.updateUser(user)
    
    suspend fun deleteUser(user: User) = 
        userDao.deleteUser(user)
    
    suspend fun updateUserRating(userId: Long, rating: Double) = 
        userDao.updateUserRating(userId, rating)
}
