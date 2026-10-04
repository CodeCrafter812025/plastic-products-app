package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductWriteBody(
    val title: String,
    val price: String,
    val weight: String,
    val color: String? = null,
    val quality: String,
    val description: String = "",
    val stock: String,
    @SerialName("is_active") val isActive: Boolean,
    val category: String? = null,
    @SerialName("sub_category") val subCategory: String? = null,
    val brand: String? = null,
    @SerialName("unit_label") val unitLabel: String? = null,
    @SerialName("packaging_info") val packagingInfo: String? = null,
    @SerialName("is_bestseller") val isBestseller: Boolean = false,
)

@Serializable
data class ProductUpdateBody(
    val title: String,
    val weight: String,
    val color: String? = null,
    val quality: String,
    val description: String = "",
    @SerialName("is_active") val isActive: Boolean,
    val category: String? = null,
    @SerialName("sub_category") val subCategory: String? = null,
    val brand: String? = null,
    @SerialName("unit_label") val unitLabel: String? = null,
    @SerialName("packaging_info") val packagingInfo: String? = null,
    @SerialName("is_bestseller") val isBestseller: Boolean = false,
)

@Serializable
data class UpdateProductPriceRequest(val price: String)

@Serializable
data class UpdateProductStockRequest(val stock: String, val reason: StockChangeReason)

@Serializable
enum class StockChangeReason {
    @SerialName("initial") INITIAL,
    @SerialName("sale") SALE,
    @SerialName("restock") RESTOCK,
    @SerialName("adjustment") ADJUSTMENT,
}

@Serializable
data class ToggleProductActiveResponse(@SerialName("is_active") val isActive: Boolean)

@Serializable
data class ProductActionMessageResponse(val message: String)

@Serializable
data class UploadProductImageResponse(
    val message: String,
    val url: String,
    @SerialName("image_urls") val imageUrls: List<String>,
)
