package com.loaderapp.domain.usecase.base

import com.loaderapp.core.common.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base UseCase for suspend operations returning [Result].
 * Automatically switches to [dispatcher] (default IO) and wraps exceptions.
 */
abstract class UseCase<in Input, out Output>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(params: Input): Result<@UnsafeVariance Output> {
        return try {
            withContext(dispatcher) { execute(params) }
        } catch (e: Exception) {
            Result.Error(message = e.message ?: "Неизвестная ошибка", exception = e)
        }
    }

    @Throws(Exception::class)
    protected abstract suspend fun execute(params: Input): Result<@UnsafeVariance Output>
}

/**
 * UseCase without parameters.
 */
abstract class NoParamsUseCase<out Output>(
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UseCase<Unit, Output>(dispatcher) {
    suspend operator fun invoke(): Result<@UnsafeVariance Output> = invoke(Unit)
}

/**
 * Base UseCase for Flow-based operations.
 *
 * [invoke] is **not** suspend: building a Flow is not a suspending operation.
 * The Flow itself may internally use suspend functions when collected.
 */
abstract class FlowUseCase<in Input, out Output> {
    operator fun invoke(params: Input): Output = execute(params)
    protected abstract fun execute(params: Input): Output
}

/**
 * FlowUseCase without parameters.
 */
abstract class NoParamsFlowUseCase<out Output> : FlowUseCase<Unit, Output>() {
    operator fun invoke(): Output = invoke(Unit)
}
