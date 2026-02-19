package com.loaderapp.ui.orders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.presentation.dispatcher.DispatcherViewModel
import com.loaderapp.presentation.loader.LoaderViewModel
import com.loaderapp.ui.dispatcher.DispatcherScreen
import com.loaderapp.ui.loader.LoaderScreen

/**
 * Обёртка вкладки «Заказы».
 * Роутит к экрану диспетчера или грузчика в зависимости от роли.
 */
@Composable
fun OrdersScreen(
    userId: Long,
    isDispatcher: Boolean,
    onOrderClick: (Long) -> Unit
) {
    if (isDispatcher) {
        val viewModel: DispatcherViewModel = hiltViewModel()
        LaunchedEffect(userId) { viewModel.initialize(userId) }
        DispatcherScreen(
            viewModel = viewModel,
            onSwitchRole = {},
            onDarkThemeChanged = {},
            onOrderClick = onOrderClick
        )
    } else {
        val viewModel: LoaderViewModel = hiltViewModel()
        LaunchedEffect(userId) { viewModel.initialize(userId) }
        LoaderScreen(
            viewModel = viewModel,
            onSwitchRole = {},
            onDarkThemeChanged = {},
            onOrderClick = onOrderClick
        )
    }
}
