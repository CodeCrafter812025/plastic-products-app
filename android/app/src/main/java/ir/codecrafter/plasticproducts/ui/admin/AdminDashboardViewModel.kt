package ir.codecrafter.plasticproducts.ui.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.LowStockProduct
import ir.codecrafter.plasticproducts.data.model.RevenueReport
import ir.codecrafter.plasticproducts.data.model.SignupCount
import ir.codecrafter.plasticproducts.data.model.TopProduct
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AdminReportsRepository
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDashboardUiState(
    val orderCounts: Map<String, Int> = emptyMap(),
    val isLoadingOrderCounts: Boolean = true,
    val orderCountsError: String? = null,

    /** Both null until the admin picks dates via the date pickers — "نمایش" stays disabled until then. */
    val revenueFrom: String? = null,
    val revenueTo: String? = null,
    val revenueReport: RevenueReport? = null,
    val isLoadingRevenue: Boolean = false,
    val revenueError: String? = null,

    val topProducts: List<TopProduct> = emptyList(),
    val isLoadingTopProducts: Boolean = true,
    val topProductsError: String? = null,

    val lowStock: List<LowStockProduct> = emptyList(),
    val isLoadingLowStock: Boolean = true,
    val lowStockError: String? = null,

    val signupsPeriod: String = "day",
    val signups: List<SignupCount> = emptyList(),
    val isLoadingSignups: Boolean = true,
    val signupsError: String? = null,
)

/**
 * Each section loads independently (its own loading/error state) — this is a
 * dashboard of otherwise-unrelated reports, not one request whose failure
 * should block the others.
 */
@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminReportsRepository: AdminReportsRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadOrderCounts()
        loadTopProducts()
        loadLowStock()
        loadSignups()
    }

    fun loadOrderCounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOrderCounts = true, orderCountsError = null) }
            when (val result = adminReportsRepository.getOrderCounts()) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoadingOrderCounts = false, orderCounts = result.data)
                }
                else -> _uiState.update {
                    it.copy(isLoadingOrderCounts = false, orderCountsError = describeFailure(result))
                }
            }
        }
    }

    fun onRevenueFromChange(date: String) = _uiState.update { it.copy(revenueFrom = date) }

    fun onRevenueToChange(date: String) = _uiState.update { it.copy(revenueTo = date) }

    fun loadRevenue() {
        val state = _uiState.value
        val from = state.revenueFrom ?: return
        val to = state.revenueTo ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRevenue = true, revenueError = null) }
            when (val result = adminReportsRepository.getRevenue(from, to)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoadingRevenue = false, revenueReport = result.data)
                }
                else -> _uiState.update {
                    it.copy(isLoadingRevenue = false, revenueError = describeFailure(result))
                }
            }
        }
    }

    fun loadTopProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTopProducts = true, topProductsError = null) }
            when (val result = adminReportsRepository.getTopProducts()) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoadingTopProducts = false, topProducts = result.data)
                }
                else -> _uiState.update {
                    it.copy(isLoadingTopProducts = false, topProductsError = describeFailure(result))
                }
            }
        }
    }

    fun loadLowStock() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLowStock = true, lowStockError = null) }
            when (val result = adminReportsRepository.getLowStock()) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoadingLowStock = false, lowStock = result.data)
                }
                else -> _uiState.update {
                    it.copy(isLoadingLowStock = false, lowStockError = describeFailure(result))
                }
            }
        }
    }

    fun onSignupsPeriodChange(period: String) {
        _uiState.update { it.copy(signupsPeriod = period) }
        loadSignups()
    }

    fun loadSignups() {
        val period = _uiState.value.signupsPeriod
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSignups = true, signupsError = null) }
            when (val result = adminReportsRepository.getSignups(period)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isLoadingSignups = false, signups = result.data)
                }
                else -> _uiState.update {
                    it.copy(isLoadingSignups = false, signupsError = describeFailure(result))
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
