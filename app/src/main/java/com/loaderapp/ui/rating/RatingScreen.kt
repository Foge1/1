package com.loaderapp.ui.rating

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.presentation.rating.RatingViewModel
import com.loaderapp.ui.components.AppScaffold
import com.loaderapp.ui.components.PlaceholderContent

@Composable
fun RatingScreen(
    viewModel: RatingViewModel = hiltViewModel()
) {
    AppScaffold(title = "Рейтинг") {
        PlaceholderContent(
            icon     = Icons.Default.Star,
            title    = "Рейтинг",
            subtitle = "Здесь будет отображаться рейтинг грузчиков",
            modifier = Modifier.fillMaxSize()
        )
    }
}
