package com.loaderapp.presentation.common

import com.loaderapp.R
import com.loaderapp.core.common.AppError
import com.loaderapp.core.common.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorUiTextMapperTest {
    @Test
    fun `no internet maps to resource`() {
        assertEquals(UiText.Resource(R.string.error_no_internet), AppError.Network.NoInternet.toUiText())
    }

    @Test
    fun `timeout maps to resource`() {
        assertEquals(UiText.Resource(R.string.error_timeout), AppError.Network.Timeout.toUiText())
    }

    @Test
    fun `unauthorized auth maps to resource`() {
        assertEquals(UiText.Resource(R.string.error_unauthorized), AppError.Auth.Unauthorized.toUiText())
    }

    @Test
    fun `unknown maps to resource`() {
        assertEquals(UiText.Resource(R.string.error_unknown), AppError.Unknown(null).toUiText())
    }

    @Test
    fun `validation with message maps to dynamic`() {
        val result = AppError.Validation(message = "Неверное имя").toUiText()
        assertTrue(result is UiText.Dynamic)
        assertEquals("Неверное имя", (result as UiText.Dynamic).value)
    }
}
