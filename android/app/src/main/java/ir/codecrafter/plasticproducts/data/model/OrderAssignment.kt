package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * orders/serializers.py OrderAssignmentSerializer — success response (201) of
 * POST order-assignments/. order_detail is the full nested OrderSerializer
 * shape, not just an id, so it reuses the existing Order model directly.
 */
@Serializable
data class OrderAssignment(
    val id: Int,
    val order: Int,
    @SerialName("order_detail") val orderDetail: Order,
    @SerialName("old_visitor") val oldVisitor: Int? = null,
    @SerialName("new_visitor") val newVisitor: Int,
    @SerialName("new_visitor_name") val newVisitorName: String? = null,
    @SerialName("assigned_by") val assignedBy: Int,
    @SerialName("assigned_by_name") val assignedByName: String? = null,
    val reason: String = "",
    @SerialName("assigned_at") val assignedAt: String,
)

/**
 * Body for POST order-assignments/ — orders/views.py OrderAssignmentViewSet.create()
 * reads these as raw dict keys (request.data.get(...)), not through a
 * serializer's validated_data, so the field names must match exactly:
 * order_id, new_visitor_id, and an optional reason (defaults to '' server-side
 * when omitted). Only a 'pending' order and an active visitor are accepted —
 * anything else fails with a 400 and a ready-to-display Persian message.
 */
@Serializable
data class AssignOrderRequest(
    @SerialName("order_id") val orderId: Int,
    @SerialName("new_visitor_id") val newVisitorId: Int,
    val reason: String? = null,
)
