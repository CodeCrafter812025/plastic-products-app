package ir.codecrafter.plasticproducts.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val phone: String = "",
    val fullName: String = "",
    val address: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccessMessage: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
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
                        saveSuccessMessage = "تغییرات ذخیره شد",
                    )
                }
                else -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = describeFailure(result),
                )
            }
        }
    }

    private fun describeFailure(result: AuthResult<*>): String = when (result) {
        is AuthResult.RateLimited -> result.message ?: "تعداد درخواست‌ها بیش از حد مجاز است، کمی بعد امتحان کنید"
        is AuthResult.Error -> when (val message = result.message) {
            is ErrorMessage.StringMessage -> message.value
            is ErrorMessage.FieldErrors -> message.fields.values.flatten().firstOrNull()
                ?: "خطایی رخ داد. دوباره تلاش کنید."
            null -> "خطایی رخ داد. دوباره تلاش کنید."
        }
        AuthResult.NetworkError -> "خطا در اتصال به اینترنت. دوباره تلاش کنید."
        is AuthResult.Success -> "" // never reached — callers only pass non-Success results here
    }
}
