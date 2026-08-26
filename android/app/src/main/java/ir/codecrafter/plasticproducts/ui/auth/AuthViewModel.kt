package ir.codecrafter.plasticproducts.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AuthRepository
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Raw values OTPRequestSerializer/OTPVerifySerializer accept for "purpose". */
object AuthPurpose {
    const val LOGIN = "login"
    const val REGISTER = "register"
}

data class AuthUiState(
    val purpose: String? = null,
    val phone: String = "",
    val fullName: String = "",
    val otpCode: String = "",
    val isRequestingOtp: Boolean = false,
    val isVerifyingOtp: Boolean = false,
    val phoneError: String? = null,
    val otpError: String? = null,
    val otpRequestError: String? = null,
    val isRateLimited: Boolean = false,
    val rateLimitMessage: String? = null,
    val resendCooldownSecondsRemaining: Int = 0,
)

sealed class AuthNavigationEvent {
    data class VerifiedSuccessfully(val role: String) : AuthNavigationEvent()
}

private const val RESEND_COOLDOWN_SECONDS = 60

// Iran mobile numbers: 11 digits, starting with 09.
private val IRAN_MOBILE_REGEX = Regex("^09\\d{9}$")

/**
 * Shared across AuthChoiceScreen, PhoneEntryScreen and OtpVerifyScreen (scoped to the
 * "auth" nav graph via hiltViewModel(navController.getBackStackEntry(AuthRoutes.GRAPH))
 * in AuthGraph.kt). One ViewModel rather than three: the screens are sequential steps
 * of a single wizard where each step needs data collected by the previous one (purpose
 * chosen in step 1 is needed by steps 2 and 3; phone entered in step 2 is needed by
 * step 3), so a graph-scoped shared ViewModel avoids re-threading that state through
 * navigation arguments and gives AuthRepository/AuthResult handling a single home.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _navigationEvents = Channel<AuthNavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    private var cooldownJob: Job? = null

    fun selectPurpose(purpose: String) {
        _uiState.value = _uiState.value.copy(purpose = purpose)
    }

    fun onPhoneChange(value: String) {
        _uiState.value = _uiState.value.copy(phone = value, phoneError = null)
    }

    fun onCodeChange(value: String) {
        _uiState.value = _uiState.value.copy(otpCode = value, otpError = null)
    }

    fun onFullNameChange(value: String) {
        _uiState.value = _uiState.value.copy(fullName = value)
    }

    fun requestOtp(onSent: () -> Unit) {
        val state = _uiState.value
        val purpose = state.purpose ?: return
        if (!IRAN_MOBILE_REGEX.matches(state.phone)) {
            _uiState.value = state.copy(phoneError = "شماره موبایل معتبر نیست (باید ۱۱ رقم و با ۰۹ شروع شود)")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRequestingOtp = true,
                phoneError = null,
                otpRequestError = null,
                isRateLimited = false,
                rateLimitMessage = null,
            )
            when (val result = authRepository.requestOtp(state.phone, purpose)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isRequestingOtp = false)
                    startResendCooldown()
                    onSent()
                }
                is AuthResult.RateLimited -> {
                    _uiState.value = _uiState.value.copy(
                        isRequestingOtp = false,
                        isRateLimited = true,
                        rateLimitMessage = RATE_LIMIT_MESSAGE,
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isRequestingOtp = false,
                        otpRequestError = describeError(result),
                    )
                }
                AuthResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(
                        isRequestingOtp = false,
                        otpRequestError = NETWORK_ERROR_MESSAGE,
                    )
                }
            }
        }
    }

    /** Re-sends using the same requestOtp() path; only reachable once the cooldown hits 0 (see OtpVerifyScreen). */
    fun resendOtp() {
        if (_uiState.value.resendCooldownSecondsRemaining > 0) return
        requestOtp(onSent = {})
    }

    fun verifyOtp() {
        val state = _uiState.value
        val purpose = state.purpose ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifyingOtp = true, otpError = null)
            val result = authRepository.verifyOtp(
                phone = state.phone,
                code = state.otpCode,
                purpose = purpose,
                fullName = state.fullName.takeIf { purpose == AuthPurpose.REGISTER && it.isNotBlank() },
            )
            when (result) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isVerifyingOtp = false)
                    _navigationEvents.send(AuthNavigationEvent.VerifiedSuccessfully(result.data.user.role))
                }
                is AuthResult.RateLimited -> {
                    _uiState.value = _uiState.value.copy(isVerifyingOtp = false, otpError = RATE_LIMIT_MESSAGE)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(isVerifyingOtp = false, otpError = describeError(result))
                }
                AuthResult.NetworkError -> {
                    _uiState.value = _uiState.value.copy(isVerifyingOtp = false, otpError = NETWORK_ERROR_MESSAGE)
                }
            }
        }
    }

    private fun startResendCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (remaining in RESEND_COOLDOWN_SECONDS downTo 1) {
                _uiState.value = _uiState.value.copy(resendCooldownSecondsRemaining = remaining)
                delay(1000)
            }
            _uiState.value = _uiState.value.copy(resendCooldownSecondsRemaining = 0)
        }
    }

    /**
     * verify_otp() in backend/users/views.py returns business errors as a bare code
     * string in error.message (e.g. "OTP_EXPIRED"), not a human-readable sentence —
     * translate the ones that view actually emits. Anything else (e.g. a field-error
     * dict from otp/request's serializer validation) falls back to its raw text.
     */
    private fun describeError(error: AuthResult.Error): String {
        val raw = when (val message = error.message) {
            is ErrorMessage.StringMessage -> message.value
            is ErrorMessage.FieldErrors -> message.fields.values.flatten().firstOrNull()
            null -> null
        }
        return when (raw) {
            "OTP_EXPIRED" -> "کد تأیید منقضی شده است. کد جدید بگیرید."
            "OTP_INVALID" -> "کد تأیید اشتباه است."
            "ACCOUNT_LOCKED" -> "حساب شما موقتاً قفل شده است."
            "ACCOUNT_INACTIVE" -> "حساب شما غیرفعال است."
            "PHONE_NOT_REGISTERED" -> "این شماره ثبت‌نام نشده است. ابتدا ثبت‌نام کنید."
            null -> "خطایی رخ داد. دوباره تلاش کنید."
            else -> raw
        }
    }

    override fun onCleared() {
        cooldownJob?.cancel()
        super.onCleared()
    }

    private companion object {
        // Deliberately not parsed from DRF's English throttle message ("Request was
        // throttled. Expected available in N seconds.") — that format is an
        // implementation detail of DRF's Throttled exception, not part of the API
        // contract, and parsing it would be fragile.
        const val RATE_LIMIT_MESSAGE = "تعداد درخواست‌ها بیش از حد مجاز است، کمی بعد امتحان کنید"
        const val NETWORK_ERROR_MESSAGE = "خطا در اتصال به اینترنت. دوباره تلاش کنید."
    }
}
