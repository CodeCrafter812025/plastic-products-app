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
