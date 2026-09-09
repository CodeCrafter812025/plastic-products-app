package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.data.model.PriceHistory
import ir.codecrafter.plasticproducts.data.model.Product
import ir.codecrafter.plasticproducts.data.model.ProductActionMessageResponse
import ir.codecrafter.plasticproducts.data.model.ProductWriteBody
import ir.codecrafter.plasticproducts.data.model.StockHistory
import ir.codecrafter.plasticproducts.data.model.ToggleProductActiveResponse
import ir.codecrafter.plasticproducts.data.model.UpdateProductPriceRequest
import ir.codecrafter.plasticproducts.data.model.UpdateProductStockRequest
import ir.codecrafter.plasticproducts.data.model.UploadProductImageResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface AdminProductApi {

    @POST("products/")
    suspend fun createProduct(@Body body: ProductWriteBody): Response<ApiEnvelope<Product>>

    @PUT("products/{id}/")
    suspend fun putProduct(@Path("id") id: Int, @Body body: ProductWriteBody): Response<ApiEnvelope<Product>>

    @PATCH("products/{id}/")
    suspend fun patchProduct(@Path("id") id: Int, @Body body: ProductWriteBody): Response<ApiEnvelope<Product>>

    /**
     * orders/models.py OrderItem.product is on_delete=models.RESTRICT — deleting a
     * product still referenced by any order item is blocked at the database level
     * with a Django RestrictedError. core/exception_handlers.py's
     * custom_exception_handler explicitly catches RestrictedError/ProtectedError
     * and turns it into a clean HTTP 400 in the standard envelope, with a
     * ready-to-display Persian message ("این محصول قبلاً در سفارشی استفاده شده و
     * قابل حذف نیست؛ به‌جای حذف، آن را غیرفعال کنید."), not a raw 500 — verified
     * by reading both orders/models.py and core/exception_handlers.py on
     * origin/main, not assumed. See AdminProductRepository.deleteProduct's KDoc
     * for the UI-layer implication (still prefer toggleActive() as the primary
     * "remove" action; delete stays a valid, cleanly-erroring secondary path).
     */
    @DELETE("products/{id}/")
    suspend fun deleteProduct(@Path("id") id: Int): Response<Unit>

    @PATCH("products/{id}/price/")
    suspend fun updatePrice(
        @Path("id") id: Int,
        @Body body: UpdateProductPriceRequest,
    ): Response<ApiEnvelope<ProductActionMessageResponse>>

    @PATCH("products/{id}/stock/")
    suspend fun updateStock(
        @Path("id") id: Int,
        @Body body: UpdateProductStockRequest,
    ): Response<ApiEnvelope<ProductActionMessageResponse>>

    /** No request body — products/views.py toggle() just flips is_active server-side. */
    @PATCH("products/{id}/toggle/")
    suspend fun toggleActive(@Path("id") id: Int): Response<ApiEnvelope<ToggleProductActiveResponse>>

    /**
     * Single file per call — products/views.py upload_image() reads
     * request.FILES['image'] (one key, not a list), so multiple images require
     * multiple calls. Only jpg/jpeg/png/gif/webp are accepted server-side, and
     * the 5-image cap is enforced there too (see ProductWriteBody's KDoc).
     */
    @Multipart
    @POST("products/{id}/upload_image/")
    suspend fun uploadImage(
        @Path("id") id: Int,
        @Part image: MultipartBody.Part,
    ): Response<ApiEnvelope<UploadProductImageResponse>>

    /**
     * Flat, unfiltered list — PriceHistoryViewSet.queryset is
     * PriceHistory.objects.all() with no get_queryset() override and no filter
     * backend configured anywhere in this backend (checked settings.py's
     * REST_FRAMEWORK block on origin/main), so there is no query param that
     * narrows this to one product's history. See
     * AdminProductRepository.getPriceHistory for the required client-side filter.
     */
    @GET("price-histories/")
    suspend fun getPriceHistories(): Response<ApiEnvelope<List<PriceHistory>>>

    /** Same caveat as getPriceHistories() — StockHistoryViewSet has no server-side product filter either. */
    @GET("stock-histories/")
    suspend fun getStockHistories(): Response<ApiEnvelope<List<StockHistory>>>
}
