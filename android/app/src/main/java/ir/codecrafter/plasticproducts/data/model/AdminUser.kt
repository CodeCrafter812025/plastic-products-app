package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors backend/users/serializers.py UserSerializer — same shape as AuthUser/
 * UserProfile, kept as its own model per this codebase's established
 * convention (see UserProfile's KDoc) of not coupling one feature's models to
 * another's, even when identical today.
 * fields = ['id', 'phone', 'full_name', 'address', 'role', 'is_active', 'created_at', 'updated_at']
 */
@Serializable
data class AdminUser(
    val id: Int,
    val phone: String,
    @SerialName("full_name") val fullName: String?,
    val address: String? = null,
    val role: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

/** Body for POST users/create_visitor/ — VisitorCreateSerializer accepts only these two fields. */
@Serializable
data class VisitorCreateBody(
    val phone: String,
    @SerialName("full_name") val fullName: String,
)

/** POST users/{id}/toggle/'s success response — users/views.py UserViewSet.toggle_active(). */
@Serializable
data class ToggleUserActiveResponse(@SerialName("is_active") val isActive: Boolean)
