package com.loaderapp.features.chat.domain.repository

import com.loaderapp.domain.model.ChatMessageModel
import kotlinx.coroutines.flow.Flow

/**
 * Расширенный контракт репозитория чата.
 * TODO: Добавить поддержку real-time сообщений (WebSocket / Firebase).
 * Текущий ChatRepository в domain/ работает только с локальной БД Room.
 */
interface ChatFeatureRepository {
    fun getMessages(orderId: Long): Flow<List<ChatMessageModel>>
    suspend fun sendMessage(orderId: Long, senderId: Long, text: String): Result<ChatMessageModel>
    suspend fun markAsRead(messageId: Long)
    fun getUnreadCount(userId: Long): Flow<Int>
}
