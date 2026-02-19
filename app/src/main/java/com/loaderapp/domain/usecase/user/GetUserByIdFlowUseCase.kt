package com.loaderapp.domain.usecase.user

import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Параметры для получения пользователя как Flow
 */
data class GetUserByIdFlowParams(val userId: Long)

/**
 * UseCase: Получить пользователя по ID как живой Flow.
 * Используется в ProfileViewModel для реактивного обновления UI.
 */
class GetUserByIdFlowUseCase @Inject constructor(
    private val userRepository: UserRepository
) : FlowUseCase<GetUserByIdFlowParams, Flow<UserModel?>>() {

    override fun execute(params: GetUserByIdFlowParams): Flow<UserModel?> =
        userRepository.getUserByIdFlow(params.userId)
}
