package ir.codecrafter.plasticproducts.ui.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Product
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AdminProductRepository
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminProductListUiState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Id of the product whose toggle/delete action is in flight, if any — disables that row's action buttons only. */
    val actionInProgressId: Int? = null,
)

sealed class AdminProductListEvent {
    data class ActionFailed(val message: String) : AdminProductListEvent()
}

/** GET products/?include_inactive=true — admin sees both active and inactive products here. */
@HiltViewModel
class AdminProductListViewModel @Inject constructor(
    private val adminProductRepository: AdminProductRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminProductListUiState())
    val uiState: StateFlow<AdminProductListUiState> = _uiState.asStateFlow()

    private val _events = Channel<AdminProductListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = adminProductRepository.getProducts(includeInactive = true)) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    products = result.data,
                )
                else -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = describeFailure(result),
                )
            }
        }
    }

    fun toggleActive(productId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgressId = productId) }
            when (val result = adminProductRepository.toggleActive(productId)) {
                is AuthResult.Success -> _uiState.update { state ->
                    state.copy(
                        actionInProgressId = null,
                        products = state.products.map {
                            if (it.id == productId) it.copy(isActive = result.data.isActive) else it
                        },
                    )
                }
                else -> {
                    _uiState.update { it.copy(actionInProgressId = null) }
                    _events.send(AdminProductListEvent.ActionFailed(describeFailure(result)))
                }
            }
        }
    }

    fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgressId = productId) }
            when (val result = adminProductRepository.deleteProduct(productId)) {
                is AuthResult.Success -> _uiState.update { state ->
                    state.copy(
                        actionInProgressId = null,
                        products = state.products.filterNot { it.id == productId },
                    )
                }
                else -> {
                    _uiState.update { it.copy(actionInProgressId = null) }
                    _events.send(AdminProductListEvent.ActionFailed(describeFailure(result)))
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
