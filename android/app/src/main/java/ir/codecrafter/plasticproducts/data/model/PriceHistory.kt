package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * products/serializers.py PriceHistorySerializer — GET price-histories/ (flat,
 * unfiltered, unpaginated; see AdminProductRepository.getPriceHistory for the
 * client-side per-product filtering this requires). old_price/new_price are
 * DecimalField, so they arrive as JSON strings, kept as String here.
 */
@Serializable
data class PriceHistory(
    val id: Int,
    val product: Int,
    @SerialName("old_price") val oldPrice: String? = null,
    @SerialName("new_price") val newPrice: String,
    @SerialName("changed_by") val changedBy: Int,
    @SerialName("changed_by_name") val changedByName: String? = null,
    @SerialName("changed_at") val changedAt: String,
)
