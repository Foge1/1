package com.loaderapp.domain.repository

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.ChatMessageModel
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с чатом
 */
interface ChatRepository {
    
    /**
     * Получить сообщения для заказа
     */
    fun getMessagesForOrder(orderId: Long): Flow<List<ChatMessageModel>>
    
    /**
     * Отправить сообщение
     */
    suspend fun sendMessage(message: ChatMessageModel): Result<Long>
    
    /**
     * Получить количество сообщений
     */
    fun getMessageCount(orderId: Long): Flow<Int>
}
