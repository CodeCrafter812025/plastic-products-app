package ir.codecrafter.plasticproducts.ui.cart

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.CartItem
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.data.repository.CartRepository
import ir.codecrafter.plasticproducts.data.repository.OrderRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val total: String = "0",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

data class PlaceOrderResult(
    val orderId: Int,
    val totalPrice: String,
)

sealed class CartEvent {
    data class ActionFailed(val message: String) : CartEvent()
}

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val _events = Channel<CartEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Kept separate from uiState, per design: placing an order is a one-shot
    // action distinct from the cart list's own loading/error lifecycle.
    private val _isPlacingOrder = MutableStateFlow(false)
    val isPlacingOrder: StateFlow<Boolean> = _isPlacingOrder.asStateFlow()

    private val _placeOrderError = MutableStateFlow<String?>(null)
    val placeOrderError: StateFlow<String?> = _placeOrderError.asStateFlow()

    private val _placeOrderResult = MutableStateFlow<PlaceOrderResult?>(null)
    val placeOrderResult: StateFlow<PlaceOrderResult?> = _placeOrderResult.asStateFlow()

    init {
        loadCart()
    }

    fun loadCart() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = cartRepository.getCart()) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    items = result.data.items,
                    total = result.data.total,
                )
                else -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = describeFailure(result),
                )
            }
        }
    }

    fun updateQuantity(cartItemId: Int, newQuantity: String) {
        val item = _uiState.value.items.firstOrNull { it.id == cartItemId } ?: return
        viewModelScope.launch {
            val result = cartRepository.updateItem(id = cartItemId, productId = item.product, quantity = newQuantity)
            if (result is AuthResult.Success) {
                loadCart()
            } else {
                _events.send(CartEvent.ActionFailed(describeFailure(result)))
            }
        }
    }

    fun removeItem(cartItemId: Int) {
        viewModelScope.launch {
            val result = cartRepository.deleteItem(cartItemId)
            if (result is AuthResult.Success) {
                loadCart()
            } else {
                _events.send(CartEvent.ActionFailed(describeFailure(result)))
            }
        }
    }

    fun placeOrder() {
        if (_uiState.value.items.isEmpty()) return
        viewModelScope.launch {
            _isPlacingOrder.value = true
            _placeOrderError.value = null
            when (val result = orderRepository.createOrder()) {
                is AuthResult.Success -> {
                    _isPlacingOrder.value = false
                    _placeOrderResult.value = PlaceOrderResult(
                        orderId = result.data.orderId,
                        totalPrice = result.data.totalPrice,
                    )
                }
                else -> {
                    _isPlacingOrder.value = false
                    _placeOrderError.value = describeFailure(result)
                }
            }
        }
    }

    fun consumePlaceOrderResult() {
        _placeOrderResult.value = null
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
