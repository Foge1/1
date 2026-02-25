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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

            var state = awaitItem()
            if (state.sessionState !is SessionState.Error) {
                state = awaitItem()
            }
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

    @Test
    fun `Given session flow transitions When repository emits states Then uiState mirrors them deterministically`() = runTest {
        val repository = FakeAuthRepository(initialState = SessionState.Authenticating)
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.sessionState is SessionState.Authenticating)
        assertTrue(viewModel.uiState.value.isLoading)

        repository.emitSession(
            SessionState.Authenticated(User(id = 7L, name = "Nina", role = UserRoleModel.DISPATCHER))
        )
        advanceUntilIdle()

        val authenticated = viewModel.uiState.value
        assertTrue(authenticated.sessionState is SessionState.Authenticated)
        assertEquals("Nina", authenticated.user?.name)
        assertEquals(false, authenticated.isLoading)

        repository.emitSession(SessionState.Error(AppError.Network.Timeout))
        advanceUntilIdle()

        val error = viewModel.uiState.value
        assertTrue(error.sessionState is SessionState.Error)
        assertEquals("Превышено время ожидания", error.error)
        assertNull(error.user)
        assertEquals(false, error.isLoading)
    }

    private class FakeAuthRepository(
        private val loginFailure: AppError? = null,
        initialState: SessionState = SessionState.Unauthenticated
    ) : AuthRepository {

        private val state = MutableStateFlow(initialState)
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

        fun emitSession(sessionState: SessionState) {
            state.update { sessionState }
        }
    }

    class MainDispatcherRule(
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
