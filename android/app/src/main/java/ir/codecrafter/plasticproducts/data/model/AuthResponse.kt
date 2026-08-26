package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.Serializable

/**
 * Mirrors the dict returned by AuthViewSet.verify_otp() in backend/users/views.py:
 * {'token': access_token, 'user': UserSerializer(user).data[, 'message': ...]}.
 *
 * IMPORTANT: verify_otp() only ever returns an access token (str(refresh.access_token)).
 * It never returns the refresh token itself, so there is currently no refresh token to
 * store after login/register — see AuthRepository.verifyOtp and TokenAuthenticator.
 *
 * The optional "message" key (present only on the change_phone branch) is dropped by
 * kotlinx.serialization's ignoreUnknownKeys config since it isn't modeled here.
 */
@Serializable
data class AuthResponse(
    val token: String,
    val user: AuthUser,
)
