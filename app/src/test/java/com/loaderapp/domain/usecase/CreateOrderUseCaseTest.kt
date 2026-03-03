package com.loaderapp.domain.usecase

import com.loaderapp.domain.repository.OrderRepository
import com.loaderapp.domain.usecase.order.CreateOrderUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Unit тест для CreateOrderUseCase.
 * Тесты помечены как временно отключённые: сценарии требуют mockk/turbine и полной имитации репозитория заказов.
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
    @Ignore("TECH-DEBT-008: Нужны mockk/turbine для проверки success path и verify вызовов репозитория")
    fun `createOrder returns success when repository succeeds`() =
        runTest {
            assertTrue(true)
        }

    @Test
    @Ignore("TECH-DEBT-008: Нужны mockk/turbine для проверки error path и propagate ошибок")
    fun `createOrder returns error when repository throws`() =
        runTest {
            assertTrue(true)
        }
}
