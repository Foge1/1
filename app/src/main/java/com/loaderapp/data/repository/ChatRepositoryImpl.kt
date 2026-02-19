package com.loaderapp.data.repository

import com.loaderapp.core.common.Result
import com.loaderapp.data.datasource.local.ChatLocalDataSource
import com.loaderapp.data.mapper.ChatMessageMapper
import com.loaderapp.domain.model.ChatMessageModel
import com.loaderapp.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Реализация ChatRepository
 */
class ChatRepositoryImpl @Inject constructor(
    private val localDataSource: ChatLocalDataSource
) : ChatRepository {
    
    override fun getMessagesForOrder(orderId: Long): Flow<List<ChatMessageModel>> {
        return localDataSource.getMessagesForOrder(orderId)
            .map { ChatMessageMapper.toDomainList(it) }
    }
    
    override suspend fun sendMessage(message: ChatMessageModel): Result<Long> {
        return try {
            val entity = ChatMessageMapper.toEntity(message)
            val id = localDataSource.insertMessage(entity)
            Result.Success(id)
        } catch (e: Exception) {
            Result.Error("Ошибка отправки сообщения: ${e.message}", e)
        }
    }
    
    override fun getMessageCount(orderId: Long): Flow<Int> {
        return localDataSource.getMessageCount(orderId)
    }
}
