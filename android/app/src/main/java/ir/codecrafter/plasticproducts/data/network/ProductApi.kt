package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.data.model.Product
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApi {

    /**
     * GET /products/ is not paginated: ProductViewSet has no pagination_class and
     * settings.py sets no DEFAULT_PAGINATION_CLASS, so envelope.data here is a
     * plain array, not {count, next, previous, results}.
     */
    @GET("products/")
    suspend fun getProducts(
        @Query("quality") quality: String? = null,
        @Query("color") color: String? = null,
        @Query("min_price") minPrice: String? = null,
        @Query("max_price") maxPrice: String? = null,
        @Query("in_stock") inStock: Boolean? = null,
    ): Response<ApiEnvelope<List<Product>>>

    @GET("products/{id}/")
    suspend fun getProduct(@Path("id") id: Int): Response<ApiEnvelope<Product>>
}
