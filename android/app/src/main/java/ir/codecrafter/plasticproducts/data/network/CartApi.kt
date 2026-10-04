package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.data.model.Cart
import ir.codecrafter.plasticproducts.data.model.CartItem
import ir.codecrafter.plasticproducts.data.model.CartItemRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface CartApi {

    @GET("cart/")
    suspend fun getCart(): Response<ApiEnvelope<Cart>>

    @POST("cart/")
    suspend fun addCartItem(@Body body: CartItemRequest): Response<ApiEnvelope<CartItem>>

    /**
     * orders/views.py CartViewSet.update() validates the body with
     * CartItemAddSerializer, which requires product_id even though the item
     * being updated is already identified by [id] in the path — the view
     * never actually reads product_id off the body, but its absence still
     * fails validation with a 400, so it must be sent anyway.
     */
    @PATCH("cart/{id}/")
    suspend fun updateCartItem(
        @Path("id") id: Int,
        @Body body: CartItemRequest,
    ): Response<ApiEnvelope<CartItem>>

    /** 204 No Content on success — no envelope body to decode. */
    @DELETE("cart/{id}/")
    suspend fun deleteCartItem(@Path("id") id: Int): Response<Unit>
}
