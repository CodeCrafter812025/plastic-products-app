package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.data.model.Product
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApi {

    @GET("products/")
    suspend fun getProducts(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
        @Query("brand") brand: String? = null,
        @Query("is_bestseller") isBestseller: Boolean? = null,
        @Query("quality") quality: String? = null,
        @Query("color") color: String? = null,
        @Query("min_price") minPrice: String? = null,
        @Query("max_price") maxPrice: String? = null,
        @Query("in_stock") inStock: Boolean? = null,
    ): Response<ApiEnvelope<List<Product>>>

    @GET("products/{id}/")
    suspend fun getProduct(@Path("id") id: Int): Response<ApiEnvelope<Product>>
}
