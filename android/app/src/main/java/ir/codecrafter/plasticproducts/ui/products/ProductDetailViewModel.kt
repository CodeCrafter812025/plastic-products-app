package ir.codecrafter.plasticproducts.ui.products

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Product
import ir.codecrafter.plasticproducts.data.model.ProductFilter
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val product: Product? = null,
    val variants: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val quantityInput: String = "1",
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

    private val initialProductId: Int = checkNotNull(savedStateHandle[ProductRoutes.PRODUCT_ID_ARG])

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProductDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadProduct()
    }

    fun loadProduct() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = productRepository.getProductDetail(initialProductId)) {
                is AuthResult.Success -> {
                    val fetchedProduct = result.data
                    val category = fetchedProduct.category?.takeIf { it.isNotBlank() }
                    val familyVariants = if (category != null) {
                        when (val famResult = productRepository.getProducts(ProductFilter(category = category))) {
                            is AuthResult.Success -> famResult.data
                            else -> listOf(fetchedProduct)
                        }
                    } else {
                        listOf(fetchedProduct)
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            product = fetchedProduct,
                            variants = familyVariants,
                        )
                    }
                }
                else -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = describeFailure(result),
                    )
                }
            }
        }
    }

    fun onSelectVariant(variant: Product) {
        _uiState.update {
            it.copy(
                product = variant,
                addToCartError = null,
            )
        }
    }

    fun onQuantityInputChange(value: String) {
        _uiState.update { it.copy(quantityInput = value, addToCartError = null) }
    }

    fun incrementQuantity() {
        val current = _uiState.value.quantityInput.trim().toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
        val next = current.add(java.math.BigDecimal.ONE).stripTrailingZeros().toPlainString()
        onQuantityInputChange(next)
    }

    fun decrementQuantity() {
        val current = _uiState.value.quantityInput.trim().toBigDecimalOrNull() ?: java.math.BigDecimal.ONE
        if (current > java.math.BigDecimal.ONE) {
            val prev = current.subtract(java.math.BigDecimal.ONE).stripTrailingZeros().toPlainString()
            onQuantityInputChange(prev)
        }
    }

    fun addToCart() {
        val quantity = _uiState.value.quantityInput.trim()
        val selectedProductId = _uiState.value.product?.id ?: initialProductId
        if (quantity.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingToCart = true, addToCartError = null) }
            when (val result = cartRepository.addItem(productId = selectedProductId, quantity = quantity)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isAddingToCart = false) }
                    _events.send(ProductDetailEvent.AddedToCart)
                }
                else -> _uiState.update {
                    it.copy(
                        isAddingToCart = false,
                        addToCartError = describeFailure(result),
                    )
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
        is AuthResult.Success -> ""
    }
}
