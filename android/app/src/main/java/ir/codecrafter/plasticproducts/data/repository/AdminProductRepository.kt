package ir.codecrafter.plasticproducts.data.repository

import ir.codecrafter.plasticproducts.data.model.PriceHistory
import ir.codecrafter.plasticproducts.data.model.Product
import ir.codecrafter.plasticproducts.data.model.ProductActionMessageResponse
import ir.codecrafter.plasticproducts.data.model.ProductWriteBody
import ir.codecrafter.plasticproducts.data.model.StockChangeReason
import ir.codecrafter.plasticproducts.data.model.StockHistory
import ir.codecrafter.plasticproducts.data.model.ToggleProductActiveResponse
import ir.codecrafter.plasticproducts.data.model.UpdateProductPriceRequest
import ir.codecrafter.plasticproducts.data.model.UpdateProductStockRequest
import ir.codecrafter.plasticproducts.data.model.UploadProductImageResponse
import ir.codecrafter.plasticproducts.data.network.AdminProductApi
import ir.codecrafter.plasticproducts.data.network.ApiEnvelope
import ir.codecrafter.plasticproducts.data.network.ApiError
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Reuses AuthRepository's AuthResult<T> — see ProductRepository for the same choice. */
@Singleton
class AdminProductRepository @Inject constructor(
    private val adminProductApi: AdminProductApi,
    private val json: Json,
) {

    suspend fun createProduct(body: ProductWriteBody): AuthResult<Product> =
        safeCall { adminProductApi.createProduct(body) }

    /** Uses PATCH — see OrderRepository.editItems for the same PUT-vs-PATCH convention note. */
    suspend fun updateProduct(id: Int, body: ProductWriteBody): AuthResult<Product> =
        safeCall { adminProductApi.patchProduct(id, body) }

    /**
     * orders/models.py OrderItem.product is on_delete=RESTRICT — deleting a
     * product still referenced by any order fails server-side, but cleanly: a
     * 400 in the standard envelope with a ready-to-display Persian message (see
     * AdminProductApi.deleteProduct's KDoc), not a raw 500. Still, toggleActive()
     * (deactivate) should be the primary "remove product" action offered in the
     * UI — most products with any order history will hit this 400 on delete, so
     * deactivating is the action that actually succeeds for them.
     */
    suspend fun deleteProduct(id: Int): AuthResult<Unit> = safeCallNoContent { adminProductApi.deleteProduct(id) }

    suspend fun updatePrice(id: Int, price: String): AuthResult<ProductActionMessageResponse> =
        safeCall { adminProductApi.updatePrice(id, UpdateProductPriceRequest(price)) }

    suspend fun updateStock(
        id: Int,
        stock: String,
        reason: StockChangeReason,
    ): AuthResult<ProductActionMessageResponse> =
        safeCall { adminProductApi.updateStock(id, UpdateProductStockRequest(stock, reason)) }

    suspend fun toggleActive(id: Int): AuthResult<ToggleProductActiveResponse> =
        safeCall { adminProductApi.toggleActive(id) }

    suspend fun uploadImage(id: Int, image: MultipartBody.Part): AuthResult<UploadProductImageResponse> =
        safeCall { adminProductApi.uploadImage(id, image) }

    /**
     * price-histories/ has no server-side product filter (see
     * AdminProductApi.getPriceHistories' KDoc) — this fetches the full history
     * for every product and filters to productId here, client-side. Fine at the
     * current data size; would need a real backend filter (or pagination) if the
     * history table grows large enough for this to get slow.
     */
    suspend fun getPriceHistory(productId: Int): AuthResult<List<PriceHistory>> {
        return when (val result = safeCall { adminProductApi.getPriceHistories() }) {
            is AuthResult.Success -> AuthResult.Success(result.data.filter { it.product == productId })
            else -> result
        }
    }

    /** Same client-side filtering caveat as getPriceHistory() — see AdminProductApi.getStockHistories(). */
    suspend fun getStockHistory(productId: Int): AuthResult<List<StockHistory>> {
        return when (val result = safeCall { adminProductApi.getStockHistories() }) {
            is AuthResult.Success -> AuthResult.Success(result.data.filter { it.product == productId })
            else -> result
        }
    }

    private suspend fun <T> safeCall(block: suspend () -> Response<ApiEnvelope<T>>): AuthResult<T> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                val envelope = response.body()
                val data = envelope?.data
                if (envelope?.success == true && data != null) {
                    AuthResult.Success(data)
                } else {
                    AuthResult.Error(code = response.code().toString(), message = null)
                }
            } else {
                val error = parseError(response.errorBody()?.string())
                if (response.code() == 429) {
                    AuthResult.RateLimited(error?.message?.let(::describe))
                } else {
                    AuthResult.Error(
                        code = error?.code ?: response.code().toString(),
                        message = error?.message,
                    )
                }
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        }
    }

    /** DELETE /products/{id}/ returns 204 No Content on success — no envelope body to decode. */
    private suspend fun safeCallNoContent(block: suspend () -> Response<Unit>): AuthResult<Unit> {
        return try {
            val response = block()
            if (response.isSuccessful) {
                AuthResult.Success(Unit)
            } else {
                val error = parseError(response.errorBody()?.string())
                if (response.code() == 429) {
                    AuthResult.RateLimited(error?.message?.let(::describe))
                } else {
                    AuthResult.Error(
                        code = error?.code ?: response.code().toString(),
                        message = error?.message,
                    )
                }
            }
        } catch (e: IOException) {
            AuthResult.NetworkError
        }
    }

    private fun parseError(rawBody: String?): ApiError? {
        if (rawBody.isNullOrEmpty()) return null
        return try {
            json.decodeFromString(ApiEnvelope.serializer(JsonElement.serializer()), rawBody).error
        } catch (e: Exception) {
            null
        }
    }

    private fun describe(message: ErrorMessage): String = when (message) {
        is ErrorMessage.StringMessage -> message.value
        is ErrorMessage.FieldErrors -> message.fields.values.flatten().joinToString()
    }
}
