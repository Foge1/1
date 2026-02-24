package com.loaderapp.features.orders.domain.usecase

import com.loaderapp.features.orders.domain.repository.OrdersRepository
import javax.inject.Inject

data class ResponderAvailability(
    val loaderId: String,
    val isBusy: Boolean,
    val busyOrderId: Long?
)

class GetRespondersWithAvailabilityUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository
) {
    suspend operator fun invoke(responderIds: List<String>): Map<String, ResponderAvailability> {
        val uniqueIds = responderIds.distinct()
        if (uniqueIds.isEmpty()) return emptyMap()

        val busyByLoader = ordersRepository.getBusyAssignments(uniqueIds)
        return uniqueIds.associateWith { loaderId ->
            val busyOrderId = busyByLoader[loaderId]
            ResponderAvailability(
                loaderId = loaderId,
                isBusy = busyOrderId != null,
                busyOrderId = busyOrderId
            )
        }
    }
}
