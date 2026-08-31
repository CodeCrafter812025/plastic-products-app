package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body expected by SimpleJWT's TokenRefreshView at /api/token/refresh/. */
@Serializable
data class RefreshTokenBody(
    val refresh: String,
)

/**
 * Mirrors SimpleJWT's TokenRefreshSerializer response. settings.py sets
 * ROTATE_REFRESH_TOKENS=True and BLACKLIST_AFTER_ROTATION=True, so a fresh
 * "refresh" value is expected on every successful call, but it's kept nullable
 * defensively in case rotation is ever turned off.
 */
@Serializable
data class TokenPair(
    @SerialName("access") val accessToken: String,
    @SerialName("refresh") val refreshToken: String? = null,
)
