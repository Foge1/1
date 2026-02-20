package com.loaderapp.domain.usecase.chat

import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.ChatMessageModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.repository.ChatRepository
import com.loaderapp.domain.usecase.base.UseCase
import javax.inject.Inject

data class SendOrderChatMessageParams(
    val orderId: Long,
    val senderId: Long,
    val senderName: String,
    val senderRole: UserRoleModel,
    val text: String
)

class SendOrderChatMessageUseCase @Inject constructor(
    private val canAccessOrderChatUseCase: CanAccessOrderChatUseCase,
    private val chatRepository: ChatRepository
) : UseCase<SendOrderChatMessageParams, Long>() {

    override suspend fun execute(params: SendOrderChatMessageParams): Result<Long> {
        val content = params.text.trim()
        if (content.isEmpty()) {
            return Result.Error("Сообщение не может быть пустым")
        }

        when (val access = canAccessOrderChatUseCase(CanAccessOrderChatParams(params.orderId, params.senderId))) {
            is Result.Success -> if (!access.data) return Result.Error("Чат недоступен для этого заказа")
            is Result.Error -> return Result.Error(access.message, access.exception)
            is Result.Loading -> return Result.Loading
        }

        return chatRepository.sendMessage(
            ChatMessageModel(
                id = 0,
                orderId = params.orderId,
                senderId = params.senderId,
                senderName = params.senderName,
                senderRole = params.senderRole,
                text = content,
                sentAt = System.currentTimeMillis()
            )
        )
    }
}
