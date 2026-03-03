package com.loaderapp.features.chat.domain.usecase

import com.loaderapp.domain.model.ChatMessageModel
import com.loaderapp.features.chat.domain.repository.ChatFeatureRepository
import javax.inject.Inject

data class SendMessageParams(
    val orderId: Long,
    val senderId: Long,
    val text: String,
)

/**
 * UseCase для отправки сообщения в чат заказа.
 * TODO(TECH-DEBT-004): Переключить use case на real-time delivery после готовности ChatFeatureRepository;
 * done when отправка/доставка подтверждается стримом входящих событий.
 */
class SendMessageUseCase
    @Inject
    constructor(
        private val chatRepository: ChatFeatureRepository,
    ) {
        suspend operator fun invoke(params: SendMessageParams): Result<ChatMessageModel> =
            chatRepository.sendMessage(params.orderId, params.senderId, params.text)
    }
