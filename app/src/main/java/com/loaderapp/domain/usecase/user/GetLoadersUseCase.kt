package com.loaderapp.domain.usecase.user

import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.repository.UserRepository
import com.loaderapp.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase: Получить список всех грузчиков
 */
class GetLoadersUseCase @Inject constructor(
    private val userRepository: UserRepository
) : FlowUseCase<Unit, Flow<List<UserModel>>>() {
    
    override fun execute(params: Unit): Flow<List<UserModel>> {
        return userRepository.getLoaders()
    }
}
