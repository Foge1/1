package com.loaderapp.ui.rating

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.presentation.rating.RatingViewModel
import com.loaderapp.ui.components.GradientBackground
import com.loaderapp.ui.components.GradientTopBar
import com.loaderapp.ui.components.PlaceholderContent

/**
 * Экран рейтинга.
 *
 * Получает состояние из [RatingViewModel] (Hilt).
 * Заглушка — готова к наполнению когда появится RatingRepository.
 */
@Composable
fun RatingScreen(
    viewModel: RatingViewModel = hiltViewModel()
) {
    GradientBackground {
        GradientTopBar(title = "Рейтинг")

        PlaceholderContent(
            icon     = Icons.Default.Star,
            title    = "Рейтинг",
            subtitle = "Здесь будет отображаться рейтинг грузчиков",
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 56.dp)
        )
    }
}
