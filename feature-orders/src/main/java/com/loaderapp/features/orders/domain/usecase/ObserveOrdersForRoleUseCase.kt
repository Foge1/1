package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.orders.filterForUser
import com.loaderapp.features.orders.domain.repository.OrdersRepository
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Наблюдение за заказами, отфильтрованными по роли текущего пользователя.
 *
 * Используется в тестах для изолированной проверки логики фильтрации.
 * В production коде фильтрация выполняется внутри [ObserveOrderUiModelsUseCase].
 */
internal class ObserveOrdersForRoleUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val currentUserProvider: CurrentUserProvider
) {
    operator fun invoke(): Flow<List<Order>> =
        currentUserProvider.observeCurrentUser()
            .flatMapLatest { user ->
                if (user == null) {
                    flowOf(emptyList())
                } else {
                    ordersRepository.observeOrders().map { orders ->
                        orders.filterForUser(user)
                    }
                }
            }
}
