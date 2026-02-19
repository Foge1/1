package com.loaderapp.ui.orders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.presentation.dispatcher.DispatcherViewModel
import com.loaderapp.presentation.loader.LoaderViewModel
import com.loaderapp.ui.dispatcher.DispatcherScreen
import com.loaderapp.ui.loader.LoaderScreen

/**
 * Вкладка «Заказы».
 *
 * Пункт 4 исправлен: оба ViewModel создаются всегда (не условно),
 * но используется только нужный. Это стабильно с Hilt и не вызывает
 * проблем при пересоздании Composable.
 */
@Composable
fun OrdersScreen(
    userId: Long,
    isDispatcher: Boolean,
    onOrderClick: (Long) -> Unit
) {
    val dispatcherViewModel: DispatcherViewModel = hiltViewModel()
    val loaderViewModel: LoaderViewModel = hiltViewModel()

    LaunchedEffect(userId) {
        if (isDispatcher) dispatcherViewModel.initialize(userId)
        else loaderViewModel.initialize(userId)
    }

    if (isDispatcher) {
        DispatcherScreen(
            viewModel = dispatcherViewModel,
            onSwitchRole = {},
            onDarkThemeChanged = {},
            onOrderClick = onOrderClick
        )
    } else {
        LoaderScreen(
            viewModel = loaderViewModel,
            onSwitchRole = {},
            onDarkThemeChanged = {},
            onOrderClick = onOrderClick
        )
    }
}
