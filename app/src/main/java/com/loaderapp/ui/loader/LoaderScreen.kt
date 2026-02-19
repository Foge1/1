package com.loaderapp.ui.loader

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loaderapp.presentation.loader.LoaderViewModel
import com.loaderapp.ui.components.GradientBackground
import com.loaderapp.ui.components.GradientTopBar
import com.loaderapp.ui.components.PlaceholderContent

/**
 * Экран грузчика.
 *
 * Текущая версия — заглушка с градиентным фоном.
 * Вкладки «Доступные» / «Мои заказы» будут добавлены,
 * когда реализуется бэкенд-интеграция.
 *
 * @param viewModel    ViewModel грузчика (пробрасывается из MainScreen)
 * @param onOrderClick Переход к детальному экрану заказа
 */
@Composable
fun LoaderScreen(
    viewModel: LoaderViewModel,
    onOrderClick: (Long) -> Unit
) {
    GradientBackground {
        GradientTopBar(title = "Заказы")

        PlaceholderContent(
            icon     = Icons.Default.LocalShipping,
            title    = "Заказы",
            subtitle = "Здесь появятся доступные заказы",
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 56.dp)
        )
    }
}
