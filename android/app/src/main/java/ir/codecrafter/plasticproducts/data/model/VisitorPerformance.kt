package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET admin-reports/visitor-performance/ — core/views.py AdminReportsViewSet.
 * visitor_performance() returns a plain array of hand-built dicts, one per
 * visitor. avgDeliverySeconds is nullable: the backend's avg_time_subquery
 * yields no row (None) when the visitor has no delivered orders yet.
 */
@Serializable
data class VisitorPerformance(
    val id: Int,
    val phone: String,
    @SerialName("full_name") val fullName: String?,
    @SerialName("total_assigned") val totalAssigned: Int,
    val delivered: Int,
    val cancelled: Int,
    @SerialName("avg_delivery_seconds") val avgDeliverySeconds: Double?,
)
