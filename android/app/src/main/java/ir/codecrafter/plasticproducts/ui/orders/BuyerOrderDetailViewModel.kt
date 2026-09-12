package ir.codecrafter.plasticproducts.ui.orders

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.local.TokenManager
import ir.codecrafter.plasticproducts.data.model.Order
import ir.codecrafter.plasticproducts.data.model.OrderStatusHistoryEntry
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.data.repository.OrderRepository
import ir.codecrafter.plasticproducts.ui.navigation.BuyerOrderRoutes
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuyerOrderDetailUiState(
    val order: Order? = null,
    val statusHistory: List<OrderStatusHistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isCancelling: Boolean = false,
)

sealed class BuyerOrderDetailEvent {
    data class ActionFailed(val message: String) : BuyerOrderDetailEvent()
}

@HiltViewModel
class BuyerOrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    tokenManager: TokenManager,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: Int = checkNotNull(savedStateHandle[BuyerOrderRoutes.ORDER_ID_ARG])

    /**
     * This screen is reused as-is for admin's order list rows (same underlying
     * data), which also surfaces "pending" orders — but edit/cancel there are
     * buyer-only actions, so they must only show for an actual buyer viewer,
     * not just based on order.status.
     */
    val isBuyer: Boolean = tokenManager.role == "buyer"

    private val _uiState = MutableStateFlow(BuyerOrderDetailUiState())
    val uiState: StateFlow<BuyerOrderDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<BuyerOrderDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadOrder()
    }

    fun loadOrder() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            // Independent endpoints, fetched in parallel — only how their results are
            // used depends on order success (a status_history failure isn't fatal to
            // the whole screen; the order itself is the primary content, history is
            // supplementary), not the requests themselves.
            val (orderResult, historyResult) = coroutineScope {
                val order = async { orderRepository.getOrderDetail(orderId) }
                val history = async { orderRepository.getStatusHistory(orderId) }
                order.await() to history.await()
            }
            when (orderResult) {
                is AuthResult.Success -> {
                    val history = (historyResult as? AuthResult.Success)?.data.orEmpty()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        order = orderResult.data,
                        statusHistory = history,
                    )
                }
                else -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = describeFailure(orderResult),
                )
            }
        }
    }

    /** On success, re-fetches the order (loadOrder()) instead of patching local state, per design. */
    fun cancelOrder() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCancelling = true)
            val result = orderRepository.cancelOrder(orderId)
            if (result is AuthResult.Success) {
                _uiState.value = _uiState.value.copy(isCancelling = false)
                loadOrder()
            } else {
                _uiState.value = _uiState.value.copy(isCancelling = false)
                _events.send(BuyerOrderDetailEvent.ActionFailed(describeFailure(result)))
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
