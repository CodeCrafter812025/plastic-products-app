package ir.codecrafter.plasticproducts.ui.admin

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.PriceHistory
import ir.codecrafter.plasticproducts.data.model.StockHistory
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AdminProductRepository
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.ui.navigation.ProductHistoryRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductHistoryUiState(
    val priceHistory: List<PriceHistory> = emptyList(),
    val stockHistory: List<StockHistory> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * getPriceHistory()/getStockHistory() already do the client-side per-product
 * filtering (no server-side filter exists — see AdminProductRepository's KDoc
 * on those methods), so this ViewModel just fires both and combines them.
 */
@HiltViewModel
class ProductHistoryViewModel @Inject constructor(
    private val adminProductRepository: AdminProductRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val productId: Int = checkNotNull(savedStateHandle[ProductHistoryRoutes.PRODUCT_ID_ARG])

    private val _uiState = MutableStateFlow(ProductHistoryUiState())
    val uiState: StateFlow<ProductHistoryUiState> = _uiState.asStateFlow()

    // No init-time load here — ProductHistoryScreen's own LaunchedEffect(Unit)
    // triggers the first load and every return-to-screen reload alike, so there's
    // exactly one load per visit instead of one from init() plus a duplicate first one.
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val priceResult = adminProductRepository.getPriceHistory(productId)
            val stockResult = adminProductRepository.getStockHistory(productId)
            if (priceResult is AuthResult.Success && stockResult is AuthResult.Success) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        priceHistory = priceResult.data,
                        stockHistory = stockResult.data,
                    )
                }
            } else {
                val failure = if (priceResult !is AuthResult.Success) priceResult else stockResult
                _uiState.update { it.copy(isLoading = false, errorMessage = describeFailure(failure)) }
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
