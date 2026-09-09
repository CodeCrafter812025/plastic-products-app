package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * products/serializers.py StockHistorySerializer — GET stock-histories/ (flat,
 * unfiltered, unpaginated; see AdminProductRepository.getStockHistory for the
 * client-side per-product filtering this requires). old_stock/new_stock are
 * DecimalField, so they arrive as JSON strings, kept as String here. reason is
 * StockChangeReason (see AdminProduct.kt) — the exact English choice key.
 */
@Serializable
data class StockHistory(
    val id: Int,
    val product: Int,
    @SerialName("old_stock") val oldStock: String? = null,
    @SerialName("new_stock") val newStock: String,
    val reason: StockChangeReason,
    @SerialName("changed_by") val changedBy: Int,
    @SerialName("changed_by_name") val changedByName: String? = null,
    @SerialName("changed_at") val changedAt: String,
)
