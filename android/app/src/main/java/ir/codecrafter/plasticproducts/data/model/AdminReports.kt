package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET admin-reports/revenue/?from=...&to=... — core/views.py AdminReportsViewSet.
 * revenue(). totalRevenue is a raw Decimal (Sum aggregate) or a bare Python
 * int 0 when there's no matching data, sent straight through Response() with
 * no DecimalField/serializer coercion — same float-not-string caveat already
 * documented on OrderCreateResponse.totalPrice in Order.kt. Kept as String
 * regardless, relying on NetworkModule's Json(isLenient = true). from/to are
 * echoed back as the same YYYY-MM-DD strings the caller sent.
 */
@Serializable
data class RevenueReport(
    val from: String,
    val to: String,
    @SerialName("total_revenue") val totalRevenue: String,
)

/**
 * One entry of GET admin-reports/top-products/?limit=N — core/views.py
 * AdminReportsViewSet.top_products(). totalQuantitySold is a raw Decimal Sum
 * aggregate (OrderItem.quantity), same float-not-string caveat as
 * RevenueReport.totalRevenue.
 */
@Serializable
data class TopProduct(
    @SerialName("product_id") val productId: Int,
    @SerialName("product_title") val productTitle: String,
    @SerialName("total_quantity_sold") val totalQuantitySold: String,
)

/**
 * One entry of GET admin-reports/low-stock/?threshold=N — core/views.py
 * AdminReportsViewSet.low_stock(). stock is a raw Decimal from a plain
 * .values() queryset (not a serializer's DecimalField), same
 * float-not-string caveat as RevenueReport.totalRevenue.
 */
@Serializable
data class LowStockProduct(
    val id: Int,
    val title: String,
    val stock: String,
)

/**
 * One entry of GET admin-reports/signups/?period=day|week — core/views.py
 * AdminReportsViewSet.signups(). period is null only in theory: it comes
 * from TruncDate/TruncWeek(User.created_at), and created_at is
 * auto_now_add=True with no null=True on the model, so every row the ORM
 * ever creates already has it set — a null period would need a raw SQL
 * insert or direct DB tampering bypassing Django entirely. Kept nullable
 * here for correctness, not because it's expected to actually occur.
 */
@Serializable
data class SignupCount(
    val period: String?,
    val count: Int,
)

// GET admin-reports/order-counts/'s response is intentionally NOT a data
// class here — core/views.py AdminReportsViewSet.order_counts() returns a
// dynamic status->count dictionary (keys are exactly Order.STATUS_CHOICES's
// codes: pending/assigned/loading/delivered/cancelled, same literals as
// Order.status in Order.kt; the view backfills any missing status with 0,
// so all five keys are always present). A plain Map<String, Int> is the
// natural fit — see AdminReportsApi.getOrderCounts.
