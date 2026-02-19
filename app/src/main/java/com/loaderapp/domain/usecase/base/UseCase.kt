package com.loaderapp.domain.usecase.base

import com.loaderapp.core.common.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Базовый абстрактный класс для UseCase
 * Инкапсулирует выполнение бизнес-логики с автоматическим переключением на IO dispatcher
 * 
 * @param Input - тип входных параметров
 * @param Output - тип результата
 */
abstract class UseCase<in Input, out Output>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    /**
     * Выполнить UseCase
     */
    suspend operator fun invoke(params: Input): Result<@UnsafeVariance Output> {
        return try {
            withContext(dispatcher) {
                execute(params)
            }
        } catch (e: Exception) {
            Result.Error(
                message = e.message ?: "Неизвестная ошибка",
                exception = e
            )
        }
    }
    
    /**
     * Реализация бизнес-логики
     * Должна быть переопределена в конкретных UseCase
     */
    @Throws(Exception::class)
    protected abstract suspend fun execute(params: Input): Result<@UnsafeVariance Output>
}

/**
 * UseCase без параметров
 */
abstract class NoParamsUseCase<out Output>(
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UseCase<Unit, Output>(dispatcher) {
    
    suspend operator fun invoke(): Result<@UnsafeVariance Output> {
        return invoke(Unit)
    }
}

/**
 * UseCase для Flow операций (без Result wrapper)
 */
abstract class FlowUseCase<in Input, out Output> {
    
    /**
     * Выполнить UseCase и вернуть Flow
     */
    suspend operator fun invoke(params: Input): Output {
        return execute(params)
    }
    
    /**
     * Реализация бизнес-логики для Flow
     */
    protected abstract suspend fun execute(params: Input): Output
}

/**
 * FlowUseCase без параметров
 */
abstract class NoParamsFlowUseCase<out Output> : FlowUseCase<Unit, Output>() {
    
    suspend operator fun invoke(): Output {
        return invoke(Unit)
    }
}
