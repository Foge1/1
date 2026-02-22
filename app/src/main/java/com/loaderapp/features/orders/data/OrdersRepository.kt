package com.loaderapp.features.orders.data

@Deprecated(
    message = "Use domain repository interface from com.loaderapp.features.orders.domain.repository",
    replaceWith = ReplaceWith("com.loaderapp.features.orders.domain.repository.OrdersRepository")
)
typealias OrdersRepository = com.loaderapp.features.orders.domain.repository.OrdersRepository
