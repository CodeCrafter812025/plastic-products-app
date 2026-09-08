package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.data.model.MarkReadResponse
import ir.codecrafter.plasticproducts.data.model.Notification
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationApi {

    /** Plain array response (no pagination), like ProductApi.getProducts()/OrderApi.getOrders(). */
    @GET("notifications/")
    suspend fun getNotifications(): Response<ApiEnvelope<List<Notification>>>

    @POST("notifications/{id}/mark_read/")
    suspend fun markRead(@Path("id") id: Int): Response<ApiEnvelope<MarkReadResponse>>

    @POST("notifications/mark_all_read/")
    suspend fun markAllRead(): Response<ApiEnvelope<MarkReadResponse>>
}
