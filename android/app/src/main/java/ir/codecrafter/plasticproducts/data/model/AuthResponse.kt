package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors the dict returned by AuthViewSet.verify_otp() in backend/users/views.py:
 * {'token': access_token, 'refresh_token': ..., 'user': UserSerializer(user).data[, 'message': ...]}.
 *
 * The optional "message" key (present only on the change_phone branch) is dropped by
 * kotlinx.serialization's ignoreUnknownKeys config since it isn't modeled here.
 */
@Serializable
data class AuthResponse(
    val token: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: AuthUser,
)

/**
 * verify_otp()'s login branch (purpose="login" only) has two possible shapes now that
 * admins can have an optional PIN second factor (backend/users/views.py):
 *  - a full login: {'token': ..., 'refresh_token': ..., 'user': ...} (same shape as AuthResponse)
 *  - a PIN challenge: {'requires_pin': true, 'phone': ...} (the admin has admin_pin set)
 * Both are modeled as one type with every field nullable, since kotlinx.serialization
 * has no discriminator to pick a sealed subclass from — use [isPinRequired] to tell
 * which shape actually came back.
 */
@Serializable
data class VerifyOtpResponse(
    val token: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val user: AuthUser? = null,
    @SerialName("requires_pin") val requiresPin: Boolean? = null,
    val phone: String? = null,
) {
    val isPinRequired: Boolean get() = requiresPin == true
}
