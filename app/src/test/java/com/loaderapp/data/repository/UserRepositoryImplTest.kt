package com.loaderapp.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.loaderapp.core.common.Result
import com.loaderapp.data.AppDatabase
import com.loaderapp.data.datasource.local.UserLocalDataSource
import com.loaderapp.domain.model.UserModel
import com.loaderapp.domain.model.UserRoleModel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserRepositoryImplTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: UserRepositoryImpl

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()

        repository = UserRepositoryImpl(UserLocalDataSource(db.userDao()))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `createUser inserts and returns user`() =
        runTest {
            val newUser =
                UserModel(
                    id = 0,
                    name = "Test User",
                    phone = "+79990000000",
                    role = UserRoleModel.LOADER,
                    rating = 4.8,
                    birthDate = 946684800000,
                    avatarInitials = "TU",
                    createdAt = 1_700_000_000_000,
                )

            val createResult = repository.createUser(newUser)
            assertTrue(createResult is Result.Success)

            val userId = (createResult as Result.Success).data
            val getResult = repository.getUserById(userId)

            assertTrue(getResult is Result.Success)
            val createdUser = (getResult as Result.Success).data
            assertEquals("Test User", createdUser.name)
            assertEquals("+79990000000", createdUser.phone)
            assertEquals(UserRoleModel.LOADER, createdUser.role)
        }

    @Test
    fun `getUserById returns error for non-existing user`() =
        runTest {
            val result = repository.getUserById(userId = 999_999)

            assertTrue(result is Result.Error)
            assertEquals("Пользователь не найден", (result as Result.Error).message)
        }
}
