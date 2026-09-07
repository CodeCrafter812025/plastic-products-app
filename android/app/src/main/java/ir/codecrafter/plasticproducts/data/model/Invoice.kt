package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One line of Invoice.items_snapshot — orders/models.py Invoice.items_snapshot is a
 * JSONField(encoder=DjangoJSONEncoder) built in orders/views.py
 * VisitorOrderStatusViewSet.status() from raw Decimal values (product.weight,
 * item.quantity, item.unit_price, item.total_price). DjangoJSONEncoder.default()
 * converts Decimal via str(o) (verified in django/core/serializers/json.py), so —
 * unlike Cart.total/OrderCreateResponse.total_price, which bypass any Decimal
 * coercion and arrive as raw JSON numbers — every decimal-ish value here is
 * already a JSON string by the time it's stored, and stays a string on read back.
 */
@Serializable
data class InvoiceItemSnapshot(
    val title: String,
    val quality: String,
    val weight: String,
    val quantity: String,
    @SerialName("unit_price") val unitPrice: String,
    @SerialName("line_total") val lineTotal: String,
)

/** orders/serializers.py InvoiceSerializer — GET /orders/{id}/invoice/'s JSON shape. */
@Serializable
data class Invoice(
    val id: Int,
    val order: Int,
    @SerialName("invoice_number") val invoiceNumber: String,
    @SerialName("buyer_name") val buyerName: String,
    @SerialName("buyer_phone") val buyerPhone: String,
    @SerialName("items_snapshot") val itemsSnapshot: List<InvoiceItemSnapshot> = emptyList(),
    @SerialName("total_price") val totalPrice: String,
    @SerialName("issued_at") val issuedAt: String,
)
