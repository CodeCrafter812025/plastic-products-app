package ir.codecrafter.plasticproducts.ui.orders

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.EditOrderItemRequest
import ir.codecrafter.plasticproducts.data.model.OrderItem
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.data.repository.OrderRepository
import ir.codecrafter.plasticproducts.ui.navigation.OrderRoutes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderEditUiState(
    val items: List<OrderItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
)

sealed class OrderEditEvent {
    data class ActionFailed(val message: String) : OrderEditEvent()
}

@HiltViewModel
class OrderEditViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: Int = checkNotNull(savedStateHandle[OrderRoutes.ORDER_ID_ARG])

    private val _uiState = MutableStateFlow(OrderEditUiState())
    val uiState: StateFlow<OrderEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<OrderEditEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _isSavedSuccessfully = MutableStateFlow(false)
    val isSavedSuccessfully: StateFlow<Boolean> = _isSavedSuccessfully.asStateFlow()

    init {
        loadOrder()
    }

    fun loadOrder() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = orderRepository.getOrderDetail(orderId)) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = result.data.items,
                )
                else -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = describeFailure(result),
                )
            }
        }
    }

    fun updateItemQuantity(productId: Int, newQuantity: String) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.map {
                if (it.product == productId) it.copy(quantity = newQuantity) else it
            },
        )
    }

    fun removeItem(productId: Int) {
        _uiState.value = _uiState.value.copy(
            items = _uiState.value.items.filterNot { it.product == productId },
        )
    }

    fun saveChanges() {
        val items = _uiState.value.items
        if (items.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val requestItems = items.map { EditOrderItemRequest(productId = it.product, quantity = it.quantity) }
            when (val result = orderRepository.editItems(orderId, requestItems)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSaving = false, items = result.data.items)
                    _isSavedSuccessfully.value = true
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    _events.send(OrderEditEvent.ActionFailed(describeFailure(result)))
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
