package ir.codecrafter.plasticproducts.ui.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.AdminUser
import ir.codecrafter.plasticproducts.data.model.Order
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AdminOrderRepository
import ir.codecrafter.plasticproducts.data.repository.AdminUserRepository
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminOrderListUiState(
    val orders: List<Order> = emptyList(),
    /** null means "all statuses" — the filter chips' selection, applied client-side in the screen. */
    val statusFilter: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Id of the order whose admin-cancel action is in flight, if any. */
    val actionInProgressOrderId: Int? = null,
    val activeVisitors: List<AdminUser> = emptyList(),
    val isLoadingVisitors: Boolean = false,
    val isAssigning: Boolean = false,
)

sealed class AdminOrderListEvent {
    data class ActionFailed(val message: String) : AdminOrderListEvent()
    data object Assigned : AdminOrderListEvent()
}

/** GET orders/ — admin sees every order (see AdminOrderRepository.getOrders's KDoc); status filtering happens client-side. */
@HiltViewModel
class AdminOrderListViewModel @Inject constructor(
    private val adminOrderRepository: AdminOrderRepository,
    private val adminUserRepository: AdminUserRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminOrderListUiState())
    val uiState: StateFlow<AdminOrderListUiState> = _uiState.asStateFlow()

    private val _events = Channel<AdminOrderListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = adminOrderRepository.getOrders()) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, orders = result.data) }
                else -> _uiState.update { it.copy(isLoading = false, errorMessage = describeFailure(result)) }
            }
        }
    }

    fun onStatusFilterChange(status: String?) = _uiState.update { it.copy(statusFilter = status) }

    /** isActive isn't part of AdminUserRepository.getUsers's own filter — applied here, client-side, on top of role=visitor. */
    fun loadActiveVisitors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingVisitors = true) }
            when (val result = adminUserRepository.getUsers(roleFilter = "visitor")) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoadingVisitors = false, activeVisitors = result.data.filter { visitor -> visitor.isActive })
                }
                else -> {
                    _uiState.update { it.copy(isLoadingVisitors = false) }
                    _events.send(AdminOrderListEvent.ActionFailed(describeFailure(result)))
                }
            }
        }
    }

    fun assignOrder(orderId: Int, visitorId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAssigning = true) }
            when (val result = adminOrderRepository.assignOrder(orderId, visitorId, reason = null)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isAssigning = false) }
                    _events.send(AdminOrderListEvent.Assigned)
                    loadOrders()
                }
                else -> {
                    _uiState.update { it.copy(isAssigning = false) }
                    _events.send(AdminOrderListEvent.ActionFailed(describeFailure(result)))
                }
            }
        }
    }

    fun cancelOrderAdmin(orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgressOrderId = orderId) }
            when (val result = adminOrderRepository.cancelOrderAdmin(orderId)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(actionInProgressOrderId = null) }
                    loadOrders()
                }
                else -> {
                    _uiState.update { it.copy(actionInProgressOrderId = null) }
                    _events.send(AdminOrderListEvent.ActionFailed(describeFailure(result)))
                }
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
