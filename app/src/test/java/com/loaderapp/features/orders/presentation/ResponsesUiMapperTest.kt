package com.loaderapp.features.orders.presentation

import com.loaderapp.features.orders.domain.Order
import com.loaderapp.features.orders.domain.OrderActionBlockReason
import com.loaderapp.features.orders.domain.OrderApplication
import com.loaderapp.features.orders.domain.OrderApplicationStatus
import com.loaderapp.features.orders.domain.OrderStatus
import com.loaderapp.features.orders.domain.OrderTime
import com.loaderapp.features.orders.domain.Role
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsesUiMapperTest {

    @Test
    fun `required 1 with 0 selected should disable start with selection hint`() {
        val item = listOf(orderUiModel(selected = false)).toResponsesItems(emptyMap()).single()

        assertFalse(item.canStart)
        assertTrue(item.startDisabledReason?.contains("Выберите 0 из 1") == true)
    }

    @Test
    fun `required 1 with selected applicant should enable start`() {
        val item = listOf(orderUiModel(selected = true)).toResponsesItems(emptyMap()).single()

        assertTrue(item.canStart)
    }

    private fun orderUiModel(selected: Boolean): OrderUiModel {
        val status = if (selected) OrderApplicationStatus.SELECTED else OrderApplicationStatus.APPLIED
        return OrderUiModel(
            order = Order(
                id = 1L,
                title = "Переезд",
                address = "ул. Пушкина",
                pricePerHour = 100.0,
                orderTime = OrderTime.Soon,
                durationMin = 60,
                workersCurrent = if (selected) 1 else 0,
                workersTotal = 1,
                tags = emptyList(),
                meta = mapOf(Order.CREATED_AT_KEY to "0"),
                status = OrderStatus.STAFFING,
                createdByUserId = "dispatcher-1",
                applications = listOf(
                    OrderApplication(
                        orderId = 1L,
                        loaderId = "loader-1",
                        status = status,
                        appliedAtMillis = 0L,
                    )
                )
            ),
            currentUserId = "dispatcher-1",
            currentUserRole = Role.DISPATCHER,
            canApply = false,
            applyBlockReason = null,
            canWithdraw = false,
            withdrawBlockReason = null,
            canSelect = true,
            canUnselect = true,
            canStart = selected,
            startBlockReason = if (selected) null else OrderActionBlockReason.SelectedCountMismatch(selected = 0, required = 1),
            canCancel = true,
            cancelBlockReason = null,
            canComplete = false,
            completeBlockReason = null,
            canOpenChat = false
        )
    }
}
