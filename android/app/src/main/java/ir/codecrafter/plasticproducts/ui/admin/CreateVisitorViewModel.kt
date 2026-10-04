package ir.codecrafter.plasticproducts.ui.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AdminUserRepository
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateVisitorUiState(
    val phone: String = "",
    val fullName: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

sealed class CreateVisitorEvent {
    data object Created : CreateVisitorEvent()
}

/**
 * Backs the "افزودن ویزیتور جدید" dialog embedded in AdminUserListScreen — not
 * its own nav destination, since the form is only two fields. Scoped to that
 * screen's own NavBackStackEntry via hiltViewModel(), so it survives the
 * dialog being dismissed and reopened; resetForm() is called after a
 * successful create and on dismiss so the next open starts fresh.
 */
@HiltViewModel
class CreateVisitorViewModel @Inject constructor(
    private val adminUserRepository: AdminUserRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateVisitorUiState())
    val uiState: StateFlow<CreateVisitorUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateVisitorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value, errorMessage = null) }

    fun onFullNameChange(value: String) = _uiState.update { it.copy(fullName = value, errorMessage = null) }

    fun resetForm() {
        _uiState.value = CreateVisitorUiState()
    }

    /** A duplicate phone (or any other validation failure) comes back through the normal error envelope, shown as-is. */
    fun submit() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            when (val result = adminUserRepository.createVisitor(state.phone.trim(), state.fullName.trim())) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(CreateVisitorEvent.Created)
                }
                else -> _uiState.update { it.copy(isSaving = false, errorMessage = describeFailure(result)) }
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
