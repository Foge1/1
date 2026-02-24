package com.loaderapp.presentation.dispatcher

import com.loaderapp.features.orders.data.FakeOrdersRepository
import com.loaderapp.features.orders.domain.Role
import com.loaderapp.features.orders.domain.session.CurrentUser
import com.loaderapp.features.orders.domain.session.CurrentUserProvider
import com.loaderapp.features.orders.domain.usecase.CreateOrderUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateOrderViewModelTest {

    @Test
    fun `SELECT_SOON then SELECT_TODAY always ends with TODAY mode`() {
        val viewModel = buildViewModel()

        viewModel.onDayOptionSelected(OrderDayOption.SOON)
        viewModel.onDayOptionSelected(OrderDayOption.TODAY)

        assertEquals(OrderDayOption.TODAY, viewModel.uiState.value.selectedDayOption)
        assertEquals(false, viewModel.uiState.value.isSoon)
    }

    private fun buildViewModel(): CreateOrderViewModel {
        val currentUserProvider = object : CurrentUserProvider {
            private val user = CurrentUser(id = "dispatcher-1", role = Role.DISPATCHER)

            override fun observeCurrentUser(): Flow<CurrentUser?> = flowOf(user)

            override suspend fun getCurrentUserOrNull(): CurrentUser? = user

            override suspend fun requireCurrentUserOnce(): CurrentUser = user
        }

        return CreateOrderViewModel(
            createOrderUseCase = CreateOrderUseCase(FakeOrdersRepository(), currentUserProvider),
            currentUserProvider = currentUserProvider
        )
    }
}
