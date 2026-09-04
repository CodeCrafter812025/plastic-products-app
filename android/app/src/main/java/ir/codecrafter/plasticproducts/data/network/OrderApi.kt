package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.data.model.CancelOrderResponse
import ir.codecrafter.plasticproducts.data.model.EditOrderItemsRequest
import ir.codecrafter.plasticproducts.data.model.Order
import ir.codecrafter.plasticproducts.data.model.OrderCreateResponse
import ir.codecrafter.plasticproducts.data.model.OrderStatusHistoryEntry
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface OrderApi {

    /** No request body — OrderViewSet.create() builds the order entirely from the caller's current cart. */
    @POST("orders/")
    suspend fun createOrder(): Response<ApiEnvelope<OrderCreateResponse>>

    /** Plain array response (no pagination), like ProductApi.getProducts(). */
    @GET("orders/{id}/status_history/")
    suspend fun getStatusHistory(@Path("id") id: Int): Response<ApiEnvelope<List<OrderStatusHistoryEntry>>>

    /** HTTP 200 with a {message} body on success, not 204 — see CancelOrderResponse. */
    @DELETE("orders/{id}/cancel/")
    suspend fun cancelOrder(@Path("id") id: Int): Response<ApiEnvelope<CancelOrderResponse>>

    @PUT("orders/{id}/edit_items/")
    suspend fun putEditItems(
        @Path("id") id: Int,
        @Body body: EditOrderItemsRequest,
    ): Response<ApiEnvelope<Order>>

    @PATCH("orders/{id}/edit_items/")
    suspend fun patchEditItems(
        @Path("id") id: Int,
        @Body body: EditOrderItemsRequest,
    ): Response<ApiEnvelope<Order>>
}
