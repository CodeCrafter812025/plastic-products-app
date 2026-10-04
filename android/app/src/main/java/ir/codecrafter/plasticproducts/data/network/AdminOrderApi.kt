package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.data.model.AssignOrderRequest
import ir.codecrafter.plasticproducts.data.model.CancelOrderResponse
import ir.codecrafter.plasticproducts.data.model.OrderAssignment
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface AdminOrderApi {

    /**
     * orders/views.py OrderAssignmentViewSet.create() — admin-only
     * (get_permissions() restricts create/update/partial_update/destroy to
     * IsAdminUserRole). Only accepts a currently 'pending' order and an
     * active visitor; anything else fails with a 400 and a ready-to-display
     * Persian message. Success is a 201 with the full OrderAssignmentSerializer
     * shape (see OrderAssignment's KDoc) — order_detail nests the whole order,
     * now updated to status 'assigned'.
     */
    @POST("order-assignments/")
    suspend fun assignOrder(@Body body: AssignOrderRequest): Response<ApiEnvelope<OrderAssignment>>

    /**
     * orders/views.py OrderViewSet.cancel_admin() — admin-only (checked
     * manually in the view, not via get_permissions), and only accepts orders
     * currently 'assigned' or 'loading' (400 with a ready-to-display Persian
     * message otherwise). No request body. Same {'message': '...'} success
     * shape as the buyer-facing DELETE orders/{id}/cancel/ — reuses
     * CancelOrderResponse rather than duplicating an identical model.
     */
    @POST("orders/{id}/cancel_admin/")
    suspend fun cancelOrderAdmin(@Path("id") id: Int): Response<ApiEnvelope<CancelOrderResponse>>
}
