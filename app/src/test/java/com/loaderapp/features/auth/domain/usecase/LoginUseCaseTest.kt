package com.loaderapp.features.auth.domain.usecase

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

class LoginUseCaseTest {

    @Test
    fun `Given valid params When invoke Then returns AppResult Success with user`() = runTest {
        val repository = FakeAuthRepository()
        val useCase = LoginUseCase(repository)

        val result = useCase(LoginParams(name = "Nikita", role = UserRoleModel.DISPATCHER))

        assertTrue(result is AppResult.Success)
        val user = (result as AppResult.Success).data
        assertEquals("Nikita", user.name)
        assertEquals(UserRoleModel.DISPATCHER, user.role)
    }

    @Test
    fun `Given invalid params When invoke Then returns AppResult Failure Validation`() = runTest {
        val repository = FakeAuthRepository(
            error = AppError.Validation("Введите имя")
        )
        val useCase = LoginUseCase(repository)

        val result = useCase(LoginParams(name = "", role = UserRoleModel.LOADER))

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Validation)
    }

    private class FakeAuthRepository(
        private val error: AppError? = null
    ) : AuthRepository {
        private val state = MutableStateFlow<SessionState>(SessionState.Unauthenticated)

        override suspend fun restoreSession(): AppResult<Unit> = AppResult.Success(Unit)

        override suspend fun login(name: String, role: UserRoleModel): AppResult<User> {
            val e = error
            if (e != null) {
                state.value = SessionState.Error(e)
                return AppResult.Failure(e)
            }
            return AppResult.Success(User(id = 1L, name = name, role = role))
        }

        override suspend fun logout(): AppResult<Unit> = AppResult.Success(Unit)

        override fun observeSession(): Flow<SessionState> = state
    }
}
