package com.loaderapp.features.auth.presentation

import app.cash.turbine.test
import com.loaderapp.core.common.AppError
import com.loaderapp.core.common.AppResult
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.features.auth.domain.model.SessionState
import com.loaderapp.features.auth.domain.model.User
import com.loaderapp.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Given valid credentials When login Then uiState becomes authenticated`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.uiState.test {
            skipItems(1)

            viewModel.onEvent(AuthEvent.Login(name = "Ivan", role = UserRoleModel.LOADER))

            val state = awaitItem()
            assertTrue(state.sessionState is SessionState.Authenticated)
            assertEquals("Ivan", state.user?.name)
            assertEquals(false, state.isLoading)
            assertEquals(null, state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given blank name When login Then uiState exposes validation error`() = runTest {
        val repository = FakeAuthRepository(loginFailure = AppError.Validation("Bad input"))
        val viewModel = AuthViewModel(repository)

        viewModel.uiState.test {
            skipItems(1)

            viewModel.onEvent(AuthEvent.Login(name = "", role = UserRoleModel.LOADER))

            val state = awaitItem()
            assertTrue(state.sessionState is SessionState.Error)
            assertEquals("Bad input", state.error)
            assertEquals(false, state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Given active session When logout Then uiState becomes unauthenticated`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onEvent(AuthEvent.Login(name = "Daria", role = UserRoleModel.DISPATCHER))
            awaitItem()

            viewModel.onEvent(AuthEvent.Logout)
            val state = awaitItem()

            assertTrue(state.sessionState is SessionState.Unauthenticated)
            assertEquals(null, state.user)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeAuthRepository(
        private val loginFailure: AppError? = null
    ) : AuthRepository {

        private val state = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
        private var id = 1L

        override suspend fun restoreSession(): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun login(name: String, role: UserRoleModel): AppResult<User> {
            val failure = loginFailure
            if (failure != null) {
                state.value = SessionState.Error(failure)
                return AppResult.Failure(failure)
            }
            val user = User(id = id++, name = name, role = role)
            state.value = SessionState.Authenticated(user)
            return AppResult.Success(user)
        }

        override suspend fun logout(): AppResult<Unit> {
            state.value = SessionState.Unauthenticated
            return AppResult.Success(Unit)
        }

        override fun observeSession(): Flow<SessionState> = state
    }

    private class MainDispatcherRule(
        private val dispatcher: TestDispatcher = StandardTestDispatcher()
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }
}
