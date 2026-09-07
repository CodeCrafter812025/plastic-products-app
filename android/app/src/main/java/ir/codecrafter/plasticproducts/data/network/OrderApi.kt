package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.data.model.CancelOrderResponse
import ir.codecrafter.plasticproducts.data.model.EditOrderItemsRequest
import ir.codecrafter.plasticproducts.data.model.Invoice
import ir.codecrafter.plasticproducts.data.model.Order
import ir.codecrafter.plasticproducts.data.model.OrderCreateResponse
import ir.codecrafter.plasticproducts.data.model.OrderStatusHistoryEntry
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Streaming

interface OrderApi {

    /** No request body — OrderViewSet.create() builds the order entirely from the caller's current cart. */
    @POST("orders/")
    suspend fun createOrder(): Response<ApiEnvelope<OrderCreateResponse>>

    /**
     * OrderViewSet is a full ModelViewSet with no retrieve() override, so this is
     * DRF's default RetrieveModelMixin behavior — OrderSerializer's full shape (see
     * Order in Order.kt), scoped by get_queryset() to orders the caller may see
     * (their own as buyer, assigned ones as visitor, all as admin). Verified by
     * reading orders/views.py directly, not assumed from the router registration.
     */
    @GET("orders/{id}/")
    suspend fun getOrder(@Path("id") id: Int): Response<ApiEnvelope<Order>>

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

    /** orders/serializers.py InvoiceSerializer — see Invoice.kt. 404 if the order has no invoice yet (not delivered). */
    @GET("orders/{id}/invoice/")
    suspend fun getInvoice(@Path("id") id: Int): Response<ApiEnvelope<Invoice>>

    /**
     * orders/views.py invoice_pdf() returns a raw django.http.HttpResponse
     * (Content-Type: application/pdf) on success, bypassing DRF's renderer/envelope
     * entirely — its error paths (403/404) are still normal enveloped JSON, but a
     * successful body is not JSON at all. @Streaming + ResponseBody makes Retrofit
     * hand back the raw body instead of routing it through the registered
     * kotlinx-serialization converter, which would otherwise fail trying to parse
     * PDF bytes as JSON.
     */
    @Streaming
    @GET("orders/{id}/invoice_pdf/")
    suspend fun getInvoicePdf(@Path("id") id: Int): Response<ResponseBody>
}
