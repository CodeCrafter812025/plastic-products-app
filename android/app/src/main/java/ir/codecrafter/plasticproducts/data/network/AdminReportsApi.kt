package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.data.model.LowStockProduct
import ir.codecrafter.plasticproducts.data.model.RevenueReport
import ir.codecrafter.plasticproducts.data.model.SignupCount
import ir.codecrafter.plasticproducts.data.model.TopProduct
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * core/views.py AdminReportsViewSet, admin-only. getVisitorPerformance() for
 * this same ViewSet's visitor-performance/ action already exists in
 * AdminUserApi.kt/AdminUserRepository.kt (built and UI-wired in an earlier
 * phase, before this dedicated reports split) — left there rather than moved,
 * to avoid touching already-shipped code for a purely organizational reason.
 */
interface AdminReportsApi {

    /**
     * core/views.py AdminReportsViewSet.order_counts() — keys are exactly
     * Order.STATUS_CHOICES's codes (pending/assigned/loading/delivered/
     * cancelled, same literals as Order.status in Order.kt), and the view
     * backfills any missing status with 0, so all five keys are always
     * present. Modeled as a plain Map, not a fixed-field data class — see
     * AdminReports.kt's trailing comment.
     */
    @GET("admin-reports/order-counts/")
    suspend fun getOrderCounts(): Response<ApiEnvelope<Map<String, Int>>>

    /**
     * Both from/to are required — core/views.py revenue() returns a 400 with
     * a ready-to-display Persian message if either is missing or not in
     * YYYY-MM-DD format. Ensuring that format is this call's caller's job
     * (the UI layer), not this API interface's.
     */
    @GET("admin-reports/revenue/")
    suspend fun getRevenue(
        @Query("from") from: String,
        @Query("to") to: String,
    ): Response<ApiEnvelope<RevenueReport>>

    /** limit defaults to 10 server-side if omitted or <= 0. */
    @GET("admin-reports/top-products/")
    suspend fun getTopProducts(@Query("limit") limit: Int? = null): Response<ApiEnvelope<List<TopProduct>>>

    /** threshold defaults to 10 server-side if omitted or negative. */
    @GET("admin-reports/low-stock/")
    suspend fun getLowStock(@Query("threshold") threshold: Int? = null): Response<ApiEnvelope<List<LowStockProduct>>>

    /** period defaults to "day" server-side if omitted or anything other than "day"/"week". */
    @GET("admin-reports/signups/")
    suspend fun getSignups(@Query("period") period: String? = null): Response<ApiEnvelope<List<SignupCount>>>
}
