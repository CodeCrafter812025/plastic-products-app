package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors backend/users/serializers.py OTPRequestSerializer.
 * purpose is one of "register" | "login" | "change_phone".
 */
@Serializable
data class OtpRequestBody(
    val phone: String,
    val purpose: String,
)

/**
 * Mirrors the dict returned by AuthViewSet.request_otp() in backend/users/views.py:
 * {'message': ..., 'code': <otp digits>, 'expires_in': 300}.
 *
 * "code" is a dev-only echo of the generated OTP (see the comment in request_otp())
 * so it can be tested without a real SMS gateway wired up yet; treat it as absent
 * once the backend stops including it.
 */
@Serializable
data class OtpRequestResponse(
    val message: String,
    val code: String? = null,
    @SerialName("expires_in") val expiresIn: Int,
)

/**
 * Mirrors backend/users/serializers.py OTPVerifySerializer. Unlike the task's literal
 * skeleton, "purpose" is included here because OTPVerifySerializer.purpose is a
 * required ChoiceField with no default — omitting it would fail backend validation
 * (400 INVALID_INPUT) on every call. full_name is only read by the backend when
 * purpose == "register".
 */
@Serializable
data class OtpVerifyBody(
    val phone: String,
    val code: String,
    val purpose: String,
    @SerialName("full_name") val fullName: String? = null,
)
