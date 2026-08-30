package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mirrors backend/products/serializers.py ProductSerializer exactly. Used for both
 * GET /products/ (list) and GET /products/{id}/ (detail) — ProductViewSet uses the
 * same serializer for both, so there's one shape here, not separate list/detail
 * models.
 *
 * price/weight/stock are DecimalField on the backend. DRF's global default
 * (COERCE_DECIMAL_TO_STRING, not overridden anywhere in this repo — checked
 * settings.py and every serializer) serializes them as JSON strings (e.g.
 * "25000.00"), not numbers, so they're kept as String on purpose here; converting
 * to BigDecimal belongs in the UI/domain layer, not this model.
 */
@Serializable
data class Product(
    val id: Int,
    val title: String,
    val price: String,
    val weight: String,
    val color: String? = null,
    val quality: String,
    val description: String = "",
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    val stock: String,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_by") val createdBy: Int,
    @SerialName("created_by_name") val createdByName: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

/** Product.QUALITY_CHOICES in backend/products/models.py — no English equivalents exist. */
object ProductQuality {
    const val PRIMARY = "اولیه"
    const val RECYCLED = "بازیافتی"
}
