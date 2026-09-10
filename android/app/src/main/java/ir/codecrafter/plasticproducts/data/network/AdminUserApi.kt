package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.data.model.AdminUser
import ir.codecrafter.plasticproducts.data.model.ToggleUserActiveResponse
import ir.codecrafter.plasticproducts.data.model.VisitorCreateBody
import ir.codecrafter.plasticproducts.data.model.VisitorPerformance
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AdminUserApi {

    /**
     * users/views.py UserViewSet.get_queryset() has no query-param filtering
     * (no ?role=... or similar) — for an admin caller it always returns
     * User.objects.all(). See AdminUserRepository.getUsers for the required
     * client-side role filter. Verified on origin/main, not assumed.
     */
    @GET("users/")
    suspend fun getUsers(): Response<ApiEnvelope<List<AdminUser>>>

    /** No request body — users/views.py toggle_active() just flips is_active server-side. POST, not PATCH. */
    @POST("users/{id}/toggle/")
    suspend fun toggleUserActive(@Path("id") id: Int): Response<ApiEnvelope<ToggleUserActiveResponse>>

    @POST("users/create_visitor/")
    suspend fun createVisitor(@Body body: VisitorCreateBody): Response<ApiEnvelope<AdminUser>>

    /** Admin-only — core/views.py AdminReportsViewSet.visitor_performance(). Plain array, no pagination. */
    @GET("admin-reports/visitor-performance/")
    suspend fun getVisitorPerformance(): Response<ApiEnvelope<List<VisitorPerformance>>>
}
