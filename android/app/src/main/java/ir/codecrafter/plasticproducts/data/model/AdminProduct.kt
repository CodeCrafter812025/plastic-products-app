package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Body for POST products/ and PUT/PATCH products/{id}/ — products/serializers.py
 * ProductSerializer's actual writable fields only (read_only_fields there is
 * ['id', 'created_by', 'created_at', 'updated_at']; created_by_name is also
 * read-only, StringRelatedField has no write side). image_urls is deliberately
 * NOT included here: it's only ever mutated through the separate multipart
 * upload_image endpoint (see AdminProductApi.uploadImage), never through this
 * body — keeping it out avoids a second, redundant way to set the same field.
 */
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
)

/** Body for PATCH products/{id}/price/. */
@Serializable
data class UpdateProductPriceRequest(val price: String)

/** Body for PATCH products/{id}/stock/. */
@Serializable
data class UpdateProductStockRequest(val stock: String, val reason: StockChangeReason)

/**
 * products/models.py StockHistory.REASON_CHOICES — wire values are these exact
 * English strings (the choice keys), not the Persian display labels, since this
 * value is an API contract with the server, not user-facing text on its own.
 */
@Serializable
enum class StockChangeReason {
    @SerialName("initial") INITIAL,
    @SerialName("sale") SALE,
    @SerialName("restock") RESTOCK,
    @SerialName("adjustment") ADJUSTMENT,
}

/** PATCH products/{id}/toggle/'s success response — products/views.py toggle(). */
@Serializable
data class ToggleProductActiveResponse(@SerialName("is_active") val isActive: Boolean)

/** PATCH products/{id}/price/ and PATCH products/{id}/stock/'s success response — both a hand-built {'message': '...'}. */
@Serializable
data class ProductActionMessageResponse(val message: String)

/** POST products/{id}/upload_image/'s success response — products/views.py upload_image(). */
@Serializable
data class UploadProductImageResponse(
    val message: String,
    val url: String,
    @SerialName("image_urls") val imageUrls: List<String>,
)
