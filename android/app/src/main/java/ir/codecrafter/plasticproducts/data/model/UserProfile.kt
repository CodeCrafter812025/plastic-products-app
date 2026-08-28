package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors backend/users/serializers.py UserSerializer exactly (same shape as
 * AuthUser — kept as a separate model since the profile feature shouldn't
 * depend on ui/auth's model classes even though today they're identical):
 * fields = ['id', 'phone', 'full_name', 'address', 'role', 'is_active', 'created_at', 'updated_at']
 * read_only_fields = ['id', 'created_at', 'updated_at', 'is_active', 'role']
 *
 * "phone" is technically writable per the serializer (it's not in
 * read_only_fields), but UpdateProfileBody below deliberately never includes
 * it — phone changes should go through the dedicated OTP-verified flow.
 */
@Serializable
data class UserProfile(
    val id: Int,
    val phone: String,
    @SerialName("full_name") val fullName: String?,
    val address: String? = null,
    val role: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

/** PATCH body for users/{id}/ — only the two fields this app ever edits. */
@Serializable
data class UpdateProfileBody(
    @SerialName("full_name") val fullName: String,
    val address: String? = null,
)
