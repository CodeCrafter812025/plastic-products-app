package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors backend/users/serializers.py UserSerializer exactly:
 * fields = ['id', 'phone', 'full_name', 'address', 'role', 'is_active', 'created_at', 'updated_at']
 */
@Serializable
data class AuthUser(
    val id: Int,
    val phone: String,
    @SerialName("full_name") val fullName: String?,
    val address: String? = null,
    val role: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)
