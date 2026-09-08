package ir.codecrafter.plasticproducts.ui.visitor

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Order
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.data.repository.OrderRepository
import ir.codecrafter.plasticproducts.data.repository.VisitorOrderRepository
import ir.codecrafter.plasticproducts.ui.navigation.VisitorOrderRoutes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VisitorOrderDetailUiState(
    val order: Order? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isUpdatingStatus: Boolean = false,
)

sealed class VisitorOrderDetailEvent {
    data class ActionFailed(val message: String) : VisitorOrderDetailEvent()
}

@HiltViewModel
class VisitorOrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val visitorOrderRepository: VisitorOrderRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: Int = checkNotNull(savedStateHandle[VisitorOrderRoutes.ORDER_ID_ARG])

    private val _uiState = MutableStateFlow(VisitorOrderDetailUiState())
    val uiState: StateFlow<VisitorOrderDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<VisitorOrderDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadOrder()
    }

    fun loadOrder() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = orderRepository.getOrderDetail(orderId)) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    order = result.data,
                )
                else -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = describeFailure(result),
                )
            }
        }
    }

    /** On success, re-fetches the order (loadOrder()) instead of patching local state, per design. */
    fun advanceStatus(newStatus: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingStatus = true)
            val result = visitorOrderRepository.updateOrderStatus(orderId, newStatus)
            if (result is AuthResult.Success) {
                _uiState.value = _uiState.value.copy(isUpdatingStatus = false)
                loadOrder()
            } else {
                _uiState.value = _uiState.value.copy(isUpdatingStatus = false)
                _events.send(VisitorOrderDetailEvent.ActionFailed(describeFailure(result)))
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
