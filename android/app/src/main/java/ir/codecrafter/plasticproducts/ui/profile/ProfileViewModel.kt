package ir.codecrafter.plasticproducts.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AuthRepository
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.data.repository.ProfileRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val phone: String = "",
    val fullName: String = "",
    val address: String = "",
    val role: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccessMessage: String? = null,
    val newPin: String = "",
    val confirmPin: String = "",
    val isSavingPin: Boolean = false,
    val pinErrorMessage: String? = null,
    val pinSuccessMessage: String? = null,
)

sealed class ProfileEvent {
    data object LoggedOut : ProfileEvent()
}

private const val MIN_ADMIN_PIN_LENGTH = 4

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadProfile()
    }

    fun logout() {
        authRepository.logout()
        viewModelScope.launch { _events.send(ProfileEvent.LoggedOut) }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = profileRepository.getProfile()) {
                is AuthResult.Success -> {
                    val profile = result.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        phone = profile.phone,
                        fullName = profile.fullName.orEmpty(),
                        address = profile.address.orEmpty(),
                        role = profile.role,
                    )
                }
                else -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = describeFailure(result),
                )
            }
        }
    }

    fun onFullNameChange(value: String) {
        _uiState.value = _uiState.value.copy(fullName = value, saveSuccessMessage = null)
    }

    fun onAddressChange(value: String) {
        _uiState.value = _uiState.value.copy(address = value, saveSuccessMessage = null)
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null, saveSuccessMessage = null)
            val result = profileRepository.updateProfile(
                fullName = state.fullName,
                address = state.address.ifBlank { null },
            )
            when (result) {
                is AuthResult.Success -> {
                    val profile = result.data
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        fullName = profile.fullName.orEmpty(),
                        address = profile.address.orEmpty(),
                        saveSuccessMessage = context.getString(R.string.msg_profile_saved),
                    )
                }
                else -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = describeFailure(result),
                )
            }
        }
    }

    fun onNewPinChange(value: String) {
        _uiState.value = _uiState.value.copy(newPin = value, pinErrorMessage = null, pinSuccessMessage = null)
    }

    fun onConfirmPinChange(value: String) {
        _uiState.value = _uiState.value.copy(confirmPin = value, pinErrorMessage = null, pinSuccessMessage = null)
    }

    fun setAdminPin() {
        val state = _uiState.value
        if (state.newPin.length < MIN_ADMIN_PIN_LENGTH) {
            _uiState.value = state.copy(pinErrorMessage = context.getString(R.string.error_admin_pin_too_short))
            return
        }
        if (state.newPin != state.confirmPin) {
            _uiState.value = state.copy(pinErrorMessage = context.getString(R.string.error_admin_pin_mismatch))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingPin = true, pinErrorMessage = null, pinSuccessMessage = null)
            val result = profileRepository.setAdminPin(state.newPin)
            when (result) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                    isSavingPin = false,
                    newPin = "",
                    confirmPin = "",
                    pinSuccessMessage = context.getString(R.string.msg_admin_pin_saved),
                )
                else -> _uiState.value = _uiState.value.copy(
                    isSavingPin = false,
                    pinErrorMessage = describeFailure(result),
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
