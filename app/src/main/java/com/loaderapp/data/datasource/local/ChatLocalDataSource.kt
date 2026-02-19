package com.loaderapp.data.datasource.local

import com.loaderapp.data.dao.ChatDao
import com.loaderapp.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * LocalDataSource для работы с чатом через Room
 */
class ChatLocalDataSource @Inject constructor(
    private val chatDao: ChatDao
) {
    
    fun getMessagesForOrder(orderId: Long): Flow<List<ChatMessage>> = 
        chatDao.getMessagesForOrder(orderId)
    
    suspend fun insertMessage(message: ChatMessage): Long = 
        chatDao.insertMessage(message)
    
    fun getMessageCount(orderId: Long): Flow<Int> = 
        chatDao.getMessageCount(orderId)
}
