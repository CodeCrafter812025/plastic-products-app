package ir.codecrafter.plasticproducts.ui.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.local.TokenManager
import ir.codecrafter.plasticproducts.data.model.AdminUser
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

data class AdminUserListUiState(
    val users: List<AdminUser> = emptyList(),
    /** null means "all roles" — the filter chips' selection, applied client-side in the screen. */
    val roleFilter: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Id of the user whose toggle action is in flight, if any — disables that row's button only. */
    val actionInProgressId: Int? = null,
)

sealed class AdminUserListEvent {
    data class ActionFailed(val message: String) : AdminUserListEvent()
}

/** GET users/ — admin sees every user (see AdminUserApi.getUsers's KDoc); role filtering happens client-side. */
@HiltViewModel
class AdminUserListViewModel @Inject constructor(
    private val adminUserRepository: AdminUserRepository,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /** The signed-in admin's own id — used by the screen to hide/disable that row's toggle button. */
    val currentUserId: Int? = tokenManager.userId

    private val _uiState = MutableStateFlow(AdminUserListUiState())
    val uiState: StateFlow<AdminUserListUiState> = _uiState.asStateFlow()

    private val _events = Channel<AdminUserListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = adminUserRepository.getUsers()) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, users = result.data) }
                else -> _uiState.update { it.copy(isLoading = false, errorMessage = describeFailure(result)) }
            }
        }
    }

    fun onRoleFilterChange(role: String?) = _uiState.update { it.copy(roleFilter = role) }

    fun toggleUserActive(userId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgressId = userId) }
            when (val result = adminUserRepository.toggleUserActive(userId)) {
                is AuthResult.Success -> _uiState.update { state ->
                    state.copy(
                        actionInProgressId = null,
                        users = state.users.map {
                            if (it.id == userId) it.copy(isActive = result.data.isActive) else it
                        },
                    )
                }
                else -> {
                    _uiState.update { it.copy(actionInProgressId = null) }
                    _events.send(AdminUserListEvent.ActionFailed(describeFailure(result)))
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
