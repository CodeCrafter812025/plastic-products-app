package ir.codecrafter.plasticproducts.ui.admin

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.VisitorPerformance
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AdminUserRepository
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.ui.navigation.VisitorPerformanceRoutes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VisitorPerformanceUiState(
    val entries: List<VisitorPerformance> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * GET admin-reports/visitor-performance/ always returns every visitor's stats
 * (see AdminUserApi.getVisitorPerformance's KDoc) — there is no per-visitor
 * endpoint. highlightVisitorId is only used by the screen to visually pick out
 * the row the admin navigated here for; it never filters the list.
 */
@HiltViewModel
class VisitorPerformanceViewModel @Inject constructor(
    private val adminUserRepository: AdminUserRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val highlightVisitorId: Int = checkNotNull(savedStateHandle[VisitorPerformanceRoutes.VISITOR_ID_ARG])

    private val _uiState = MutableStateFlow(VisitorPerformanceUiState())
    val uiState: StateFlow<VisitorPerformanceUiState> = _uiState.asStateFlow()

    // No init-time load here — VisitorPerformanceScreen's own LaunchedEffect(Unit)
    // triggers the first load and every return-to-screen reload alike, so there's
    // exactly one load per visit instead of one from init() plus a duplicate first one.
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = adminUserRepository.getVisitorPerformance()) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, entries = result.data) }
                else -> _uiState.update { it.copy(isLoading = false, errorMessage = describeFailure(result)) }
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
