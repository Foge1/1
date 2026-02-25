package com.loaderapp.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AppResultTest {
    @Test
    fun `toAppError maps timeout`() {
        assertEquals(AppError.Network.Timeout, SocketTimeoutException("timeout").toAppError())
    }

    @Test
    fun `toAppError maps unknown host`() {
        assertEquals(AppError.Network.UnknownHost, UnknownHostException("host").toAppError())
    }

    @Test
    fun `toAppError maps connect exception as no internet`() {
        assertEquals(AppError.Network.NoInternet, ConnectException("connection refused").toAppError())
    }

    @Test
    fun `toAppError maps io dns message as dns`() {
        assertEquals(AppError.Network.Dns, IOException("DNS lookup failed").toAppError())
    }

    @Test
    fun `appRunCatching returns success`() {
        assertEquals(AppResult.Success(42), appRunCatching { 42 })
    }

    @Test
    fun `appRunCatching returns failure with mapped error`() {
        val result = appRunCatching<Int> { throw SocketTimeoutException("boom") }
        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.Network.Timeout, (result as AppResult.Failure).error)
    }
}
