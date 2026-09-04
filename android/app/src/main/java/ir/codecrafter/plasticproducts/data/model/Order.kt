package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderItem(
    val id: Int,
    val order: Int,
    val product: Int,
    @SerialName("product_detail") val productDetail: Product,
    val quantity: String,
    @SerialName("unit_price") val unitPrice: String,
    @SerialName("total_price") val totalPrice: String,
)

/** orders/serializers.py OrderSerializer — the full order shape. */
@Serializable
data class Order(
    val id: Int,
    val buyer: Int,
    @SerialName("buyer_name") val buyerName: String? = null,
    val visitor: Int? = null,
    @SerialName("visitor_name") val visitorName: String? = null,
    @SerialName("total_price") val totalPrice: String,
    val status: String,
    val items: List<OrderItem> = emptyList(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

/**
 * POST /orders/'s success response — orders/views.py OrderViewSet.create()
 * returns a hand-built dict, not OrderSerializer, so its shape (and its
 * types) differ from [Order]. In particular total_price here is a raw
 * Decimal read straight off the model instance, bypassing DecimalField's
 * string coercion — the wire value is a JSON number, not a string, same
 * caveat as Cart.total/CartItem.subtotal (see Cart.kt). Kept as String only
 * because NetworkModule's Json has isLenient = true.
 */
@Serializable
data class OrderCreateResponse(
    @SerialName("order_id") val orderId: Int,
    val status: String,
    @SerialName("total_price") val totalPrice: String,
    val message: String,
)

/** GET /orders/{id}/status_history/ — orders/serializers.py OrderStatusHistorySerializer, returned as a plain array. */
@Serializable
data class OrderStatusHistoryEntry(
    val id: Int,
    val order: Int,
    @SerialName("old_status") val oldStatus: String? = null,
    @SerialName("new_status") val newStatus: String,
    @SerialName("changed_by") val changedBy: Int,
    @SerialName("changed_by_name") val changedByName: String? = null,
    val note: String = "",
    @SerialName("changed_at") val changedAt: String,
)

/** DELETE /orders/{id}/cancel/'s success response — {'message': '...'}, HTTP 200 (not 204). */
@Serializable
data class CancelOrderResponse(val message: String)

/**
 * One entry of PUT/PATCH /orders/{id}/edit_items/'s "items" body —
 * orders/views.py OrderViewSet.edit_items(). Unlike CartItemRequest, this
 * endpoint parses request.data as a raw dict with no serializer validation:
 * product_id is looked up as a dict key against integer product IDs, so it
 * must serialize as a JSON number (Int here), never a JSON string.
 *
 * quantity is kept as String for consistency with the rest of this codebase,
 * but that consistency is currently unusable for non-integer amounts: the
 * view mixes this value directly into arithmetic against Decimal model
 * fields (product.stock, the existing OrderItem.quantity) with no type
 * coercion. A whole-number JSON integer works (Decimal-int arithmetic is
 * legal in Python), but a JSON string (Decimal-str arithmetic — actually
 * str<int comparison, which is the first thing checked) or a JSON float
 * (Decimal-float arithmetic) both raise an unhandled TypeError server-side,
 * i.e. an uncaught 500, not a clean validation error. Verified by reading
 * orders/views.py's edit_items() arithmetic directly, not by guessing.
 * Until that's fixed backend-side, only whole-number quantities can safely
 * go through this endpoint.
 */
@Serializable
data class EditOrderItemRequest(
    @SerialName("product_id") val productId: Int,
    val quantity: String,
)

@Serializable
data class EditOrderItemsRequest(
    val items: List<EditOrderItemRequest>,
)
