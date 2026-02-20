package com.loaderapp.domain.usecase.chat

import com.loaderapp.domain.model.ChatMessageModel
import com.loaderapp.domain.repository.ChatRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class ObserveOrderChatMessagesParams(val orderId: Long)

class ObserveOrderChatMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) : FlowUseCase<ObserveOrderChatMessagesParams, Flow<List<ChatMessageModel>>>() {

    override fun execute(params: ObserveOrderChatMessagesParams): Flow<List<ChatMessageModel>> {
        return chatRepository.getMessagesForOrder(params.orderId)
    }
}
