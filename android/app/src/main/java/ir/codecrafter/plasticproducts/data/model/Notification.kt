package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * core/serializers.py NotificationSerializer — GET /api/v1/notifications/ returns
 * a plain array (core/views.py NotificationViewSet, no list() override, no
 * pagination_class anywhere in this backend — verified via origin/main, same as
 * ProductApi.getProducts()/OrderApi.getOrders()). related_type/related_id are a
 * generic polymorphic reference (core/models.py Notification.related_type/
 * related_id), not a typed FK — related_type must be checked against "order"
 * before treating related_id as an order id. There is no "title" field and the
 * timestamp field is sent_at, not created_at.
 */
@Serializable
data class Notification(
    val id: Int,
    val user: Int,
    @SerialName("related_type") val relatedType: String? = null,
    @SerialName("related_id") val relatedId: Long? = null,
    val type: String,
    val message: String,
    @SerialName("sent_at") val sentAt: String,
    @SerialName("is_read") val isRead: Boolean,
)

/**
 * POST /notifications/{id}/mark_read/ and POST /notifications/mark_all_read/'s
 * success response — core/views.py NotificationViewSet.mark_read()/
 * mark_all_read() both return a hand-built {'status': '...'} dict (literal
 * English confirmation strings, not meant for display), not the Notification
 * itself.
 */
@Serializable
data class MarkReadResponse(val status: String)
