package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.data.model.UpdateOrderStatusRequest
import ir.codecrafter.plasticproducts.data.model.UpdateOrderStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.Path

interface VisitorOrderApi {

    /**
     * VisitorOrderStatusViewSet is a GenericViewSet with no list/retrieve mixins —
     * this "status" action is the only endpoint it exposes. There is no GET on
     * /visitor/orders/ or /visitor/orders/{id}/ at all; a visitor's own order list
     * still comes from GET /orders/{id}/ (scoped by get_queryset() to
     * visitor=request.user) once that list endpoint exists.
     */
    @PATCH("visitor/orders/{id}/status/")
    suspend fun updateStatus(
        @Path("id") id: Int,
        @Body body: UpdateOrderStatusRequest,
    ): Response<ApiEnvelope<UpdateOrderStatusResponse>>
}
