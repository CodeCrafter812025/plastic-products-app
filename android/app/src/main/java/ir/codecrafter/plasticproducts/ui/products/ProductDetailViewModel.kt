package ir.codecrafter.plasticproducts.ui.products

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Product
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.data.repository.CartRepository
import ir.codecrafter.plasticproducts.data.repository.ProductRepository
import ir.codecrafter.plasticproducts.ui.navigation.ProductRoutes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val product: Product? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quantityInput: String = "",
    val isAddingToCart: Boolean = false,
    val addToCartError: String? = null,
)

sealed class ProductDetailEvent {
    data object AddedToCart : ProductDetailEvent()
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val productId: Int = checkNotNull(savedStateHandle[ProductRoutes.PRODUCT_ID_ARG])

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProductDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadProduct()
    }

    fun loadProduct() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = productRepository.getProductDetail(productId)) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    product = result.data,
                )
                else -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = describeFailure(result),
                )
            }
        }
    }

    fun onQuantityInputChange(value: String) {
        _uiState.value = _uiState.value.copy(quantityInput = value, addToCartError = null)
    }

    fun addToCart() {
        val quantity = _uiState.value.quantityInput
        if (quantity.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingToCart = true, addToCartError = null)
            when (val result = cartRepository.addItem(productId = productId, quantity = quantity)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isAddingToCart = false, quantityInput = "")
                    _events.send(ProductDetailEvent.AddedToCart)
                }
                else -> _uiState.value = _uiState.value.copy(
                    isAddingToCart = false,
                    addToCartError = describeFailure(result),
                )
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
