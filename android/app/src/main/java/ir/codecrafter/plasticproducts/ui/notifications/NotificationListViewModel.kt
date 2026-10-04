package ir.codecrafter.plasticproducts.ui.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Notification
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.data.repository.NotificationRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationListUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

sealed class NotificationListEvent {
    data class NavigateToOrder(val orderId: Int) : NotificationListEvent()
    data class ActionFailed(val message: String) : NotificationListEvent()
}

/** GET /notifications/ — core/views.py NotificationViewSet already scopes this to the calling user's own notifications. */
@HiltViewModel
class NotificationListViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationListUiState())
    val uiState: StateFlow<NotificationListUiState> = _uiState.asStateFlow()

    private val _events = Channel<NotificationListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = notificationRepository.getNotifications()) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    notifications = result.data,
                )
                else -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = describeFailure(result),
                )
            }
        }
    }

    /** related_type/related_id are a generic polymorphic reference — only "order" is a known, navigable type today. */
    fun onNotificationClick(notification: Notification) {
        viewModelScope.launch {
            if (!notification.isRead) {
                when (val result = notificationRepository.markRead(notification.id)) {
                    is AuthResult.Success -> _uiState.update { state ->
                        state.copy(
                            notifications = state.notifications.map {
                                if (it.id == notification.id) it.copy(isRead = true) else it
                            },
                        )
                    }
                    else -> _events.send(NotificationListEvent.ActionFailed(describeFailure(result)))
                }
            }
            val orderId = notification.relatedId
            if (notification.relatedType == "order" && orderId != null) {
                _events.send(NotificationListEvent.NavigateToOrder(orderId.toInt()))
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            when (val result = notificationRepository.markAllRead()) {
                is AuthResult.Success -> _uiState.update { state ->
                    state.copy(notifications = state.notifications.map { it.copy(isRead = true) })
                }
                else -> _events.send(NotificationListEvent.ActionFailed(describeFailure(result)))
            }
        }
    }

    private fun describeFailure(result: AuthResult<*>): String = when (result) {
        is AuthResult.RateLimited -> result.message ?: context.getString(R.string.error_rate_limited)
        is AuthResult.Error -> when (val message = result.message) {
            is ErrorMessage.StringMessage -> message.value
            is ErrorMessage.FieldErrors -> message.fields.values.flatten().firstOrNull()
                ?: context.getString(R.string.error_generic)
            null -> context.getString(R.string.error_generic)
        }
        AuthResult.NetworkError -> context.getString(R.string.error_network)
        is AuthResult.Success -> "" // never reached — callers only pass non-Success results here
    }
}
