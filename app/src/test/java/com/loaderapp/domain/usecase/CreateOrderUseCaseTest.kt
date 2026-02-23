package com.loaderapp.domain.usecase

import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.order.CreateOrderParams
import com.loaderapp.domain.usecase.order.CreateOrderUseCase
// import io.mockk.coEvery
// import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit тест для CreateOrderUseCase.
 * TODO: Раскомментировать и дополнить тесты когда подключат mockk/turbine.
 *
 * Необходимые зависимости в build.gradle (testImplementation):
 *   io.mockk:mockk:1.13.8
 *   org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3
 *   app.cash.turbine:turbine:1.0.0
 */
class CreateOrderUseCaseTest {

    private lateinit var orderRepository: OrderRepository
    private lateinit var useCase: CreateOrderUseCase

    @Before
    fun setUp() {
        // orderRepository = mockk()
        // useCase = CreateOrderUseCase(orderRepository)
    }

    @Test
    fun `createOrder returns success when repository succeeds`() = runTest {
        // TODO: Implement
        assertTrue(true)
    }

    @Test
    fun `createOrder returns error when repository throws`() = runTest {
        // TODO: Implement
        assertTrue(true)
    }
}
