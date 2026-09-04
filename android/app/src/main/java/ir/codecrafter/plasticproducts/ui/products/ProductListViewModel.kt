package ir.codecrafter.plasticproducts.ui.products

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Product
import ir.codecrafter.plasticproducts.data.model.ProductFilter
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val searchText: String = "",
    val filter: ProductFilter = ProductFilter(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    // Every filter/search change funnels a request through here rather than
    // launching loadProducts() directly, so collectLatest below can cancel a
    // still-in-flight request when a newer one comes in — otherwise a slow
    // response to an older filter could land after and overwrite a newer one.
    private val reloadRequests = MutableSharedFlow<Unit>(replay = 1)

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(SEARCH_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collectLatest { text ->
                    _uiState.update { it.copy(filter = it.filter.copy(search = text.ifBlank { null })) }
                    reloadRequests.emit(Unit)
                }
        }
        viewModelScope.launch {
            reloadRequests.collectLatest { loadProducts() }
        }
        reloadRequests.tryEmit(Unit)
    }

    fun onSearchTextChange(text: String) {
        _uiState.update { it.copy(searchText = text) }
        searchQuery.value = text
    }

    fun onQualityChange(quality: String?) = updateFilter { it.copy(quality = quality) }

    fun onInStockOnlyChange(inStockOnly: Boolean) =
        updateFilter { it.copy(inStock = if (inStockOnly) true else null) }

    fun onMinPriceChange(value: String) = updateFilter { it.copy(minPrice = value.ifBlank { null }) }

    fun onMaxPriceChange(value: String) = updateFilter { it.copy(maxPrice = value.ifBlank { null }) }

    private fun updateFilter(transform: (ProductFilter) -> ProductFilter) {
        _uiState.update { it.copy(filter = transform(it.filter)) }
        reloadRequests.tryEmit(Unit)
    }

    private suspend fun loadProducts() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        when (val result = productRepository.getProducts(_uiState.value.filter)) {
            is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, products = result.data) }
            is AuthResult.RateLimited -> _uiState.update {
                it.copy(isLoading = false, errorMessage = result.message ?: context.getString(R.string.error_rate_limited))
            }
            is AuthResult.Error -> _uiState.update {
                it.copy(isLoading = false, errorMessage = describeError(result))
            }
            AuthResult.NetworkError -> _uiState.update {
                it.copy(isLoading = false, errorMessage = context.getString(R.string.error_network))
            }
        }
    }

    private fun describeError(error: AuthResult.Error): String = when (val message = error.message) {
        is ErrorMessage.StringMessage -> message.value
        is ErrorMessage.FieldErrors ->
            message.fields.values.flatten().firstOrNull() ?: context.getString(R.string.error_generic)
        null -> context.getString(R.string.error_generic)
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
