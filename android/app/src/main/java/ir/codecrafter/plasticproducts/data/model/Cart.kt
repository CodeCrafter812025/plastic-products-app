package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CartItem(
    val id: Int,
    val user: Int,
    val product: Int,
    @SerialName("product_detail") val productDetail: Product,
    val quantity: String,
    @SerialName("added_at") val addedAt: String,
    @SerialName("updated_at") val updatedAt: String,
    /**
     * orders/serializers.py CartItemSerializer.get_subtotal() returns a raw
     * Decimal (quantity * product.price) from a SerializerMethodField, which
     * bypasses DecimalField's string coercion — unlike every other decimal
     * field in this API, the wire value here is a JSON number, not a string.
     * Kept as String for consistency only because NetworkModule's Json has
     * isLenient = true, which allows decoding a bare JSON number into a
     * String property.
     */
    val subtotal: String,
)

/** GET /cart/'s response shape: orders/views.py CartViewSet.list(). */
@Serializable
data class Cart(
    val items: List<CartItem> = emptyList(),
    /** Same raw-Decimal-as-JSON-number caveat as [CartItem.subtotal] — see there. */
    val total: String,
)

/**
 * Body for POST /cart/ and PATCH /cart/{id}/ — orders/serializers.py
 * CartItemAddSerializer. On the update call, product_id is still required by
 * that serializer even though CartViewSet.update() never reads it (the item
 * being updated is identified by the path's {id}); omitting it fails
 * validation with a 400.
 */
@Serializable
data class CartItemRequest(
    @SerialName("product_id") val productId: Int,
    val quantity: String,
)
