package com.loaderapp.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.loaderapp.core.common.UiState
import com.loaderapp.data.mapper.UserMapper
import com.loaderapp.presentation.profile.ProfileViewModel
import com.loaderapp.ui.components.ErrorView
import com.loaderapp.ui.components.LoadingView

/**
 * Вкладка «Профиль».
 * Данные получает через ProfileViewModel → UseCases → Repositories.
 */
@Composable
fun ProfileScreen(userId: Long, isDispatcher: Boolean = false, onSwitchRole: (() -> Unit)? = null) {
    val viewModel: ProfileViewModel = hiltViewModel()

    LaunchedEffect(userId) { viewModel.initialize(userId, isDispatcher) }

    val state by viewModel.state.collectAsState()

    when (state) {
        is UiState.Loading, UiState.Idle -> LoadingView()
        is UiState.Error -> ErrorView(message = (state as UiState.Error).message)
        is UiState.Success -> {
            val data = (state as UiState.Success).data
            // Маппим domain UserModel → data User для существующего ProfileScreen
            val dataUser = UserMapper.toEntity(data.user)
            ProfileScreen(
                user = dataUser,
                completedCount = data.completedCount,
                totalEarnings = data.totalEarnings,
                averageRating = data.averageRating,
                dispatcherCompletedCount = data.dispatcherCompletedCount,
                dispatcherActiveCount = data.dispatcherActiveCount,
                onMenuClick = {},
                onSaveProfile = { name, phone, birthDate ->
                    viewModel.saveProfile(name, phone, birthDate)
                },
                onSwitchRole = onSwitchRole
            )
        }
        else -> {}
    }
}
