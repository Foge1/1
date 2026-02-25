package com.loaderapp.features.auth.presentation

import com.loaderapp.core.common.AppError
import com.loaderapp.core.common.AppResult
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.features.auth.domain.model.SessionState
import com.loaderapp.features.auth.domain.model.User
import com.loaderapp.features.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewModelTest {

    @Test
    fun `login success transitions to authenticated state`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.onEvent(AuthEvent.Login(name = "Ivan", role = UserRoleModel.LOADER))

        val state = viewModel.uiState.value
        assertTrue(state.sessionState is SessionState.Authenticated)
        assertEquals("Ivan", state.user?.name)
        assertEquals(false, state.isLoading)
        assertEquals(null, state.error)
    }

    @Test
    fun `login failure transitions to error state`() = runTest {
        val repository = FakeAuthRepository(loginFailure = AppError.Validation("Bad input"))
        val viewModel = AuthViewModel(repository)

        viewModel.onEvent(AuthEvent.Login(name = "", role = UserRoleModel.LOADER))

        val state = viewModel.uiState.value
        assertTrue(state.sessionState is SessionState.Error)
        assertEquals("Bad input", state.error)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `logout transitions to unauthenticated`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.onEvent(AuthEvent.Login(name = "Daria", role = UserRoleModel.DISPATCHER))
        viewModel.onEvent(AuthEvent.Logout)

        val state = viewModel.uiState.value
        assertTrue(state.sessionState is SessionState.Unauthenticated)
        assertEquals(null, state.user)
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
}
