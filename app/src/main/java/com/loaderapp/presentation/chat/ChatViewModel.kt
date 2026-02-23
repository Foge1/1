package com.loaderapp.presentation.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.loaderapp.core.common.Result
import com.loaderapp.domain.model.ChatMessageModel
import com.loaderapp.domain.model.UserRoleModel
import com.loaderapp.domain.usecase.chat.CanAccessOrderChatParams
import com.loaderapp.domain.usecase.chat.CanAccessOrderChatUseCase
import com.loaderapp.domain.usecase.chat.ObserveOrderChatMessagesParams
import com.loaderapp.domain.usecase.chat.ObserveOrderChatMessagesUseCase
import com.loaderapp.domain.usecase.chat.SendOrderChatMessageParams
import com.loaderapp.domain.usecase.chat.SendOrderChatMessageUseCase
import com.loaderapp.navigation.NavArgs
import com.loaderapp.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val canAccessOrderChatUseCase: CanAccessOrderChatUseCase,
    private val observeOrderChatMessagesUseCase: ObserveOrderChatMessagesUseCase,
    private val sendOrderChatMessageUseCase: SendOrderChatMessageUseCase
) : BaseViewModel() {

    private val orderId: Long = savedStateHandle.get<Any?>(NavArgs.ORDER_ID)
        ?.let(::parseOrderId)
        ?: error("ChatViewModel requires valid '${NavArgs.ORDER_ID}' in SavedStateHandle")

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentUserId: Long? = null
    private var currentUserName: String = ""
    private var currentUserRole: UserRoleModel = UserRoleModel.LOADER

    fun initialize(userId: Long, userName: String, userRole: UserRoleModel) {
        Log.d(LOG_TAG, "load orderId=$orderId")
        if (currentUserId == userId && _uiState.value.isInitialized) return
        currentUserId = userId
        currentUserName = userName
        currentUserRole = userRole
        _uiState.value = _uiState.value.copy(isInitialized = true, isLoading = true)

        launchSafe {
            when (val access = canAccessOrderChatUseCase(CanAccessOrderChatParams(orderId, userId))) {
                is Result.Success -> {
                    Log.d(LOG_TAG, "load orderId=$orderId, found=true")
                    if (!access.data) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            canChat = false,
                            error = "Чат доступен только после взятия заказа"
                        )
                        return@launchSafe
                    }

                    _uiState.value = _uiState.value.copy(isLoading = false, canChat = true, error = null)
                    observeOrderChatMessagesUseCase(ObserveOrderChatMessagesParams(orderId))
                        .onEach { messages ->
                            _uiState.value = _uiState.value.copy(messages = messages)
                        }
                        .launchIn(viewModelScope)
                }
                is Result.Error -> {
                    Log.d(LOG_TAG, "load orderId=$orderId, found=false")
                    _uiState.value = _uiState.value.copy(isLoading = false, canChat = false, error = access.message)
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onDraftChanged(value: String) {
        _uiState.value = _uiState.value.copy(draftMessage = value)
    }

    fun sendMessage() {
        val senderId = currentUserId ?: return
        val state = _uiState.value
        if (!state.canChat) return

        launchSafe {
            val result = sendOrderChatMessageUseCase(
                SendOrderChatMessageParams(
                    orderId = orderId,
                    senderId = senderId,
                    senderName = currentUserName,
                    senderRole = currentUserRole,
                    text = state.draftMessage
                )
            )
            when (result) {
                is Result.Success -> _uiState.value = _uiState.value.copy(draftMessage = "")
                is Result.Error -> showSnackbar(result.message)
                is Result.Loading -> Unit
            }
        }
    }
    private fun parseOrderId(raw: Any): Long? = when (raw) {
        is Long -> raw
        is Int -> raw.toLong()
        is String -> raw.toLongOrNull()
        else -> null
    }

    private companion object {
        const val LOG_TAG = "ChatVM"
    }

}

data class ChatUiState(
    val isInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val canChat: Boolean = false,
    val messages: List<ChatMessageModel> = emptyList(),
    val draftMessage: String = "",
    val error: String? = null
)
