package ir.codecrafter.plasticproducts.data.repository

import ir.codecrafter.plasticproducts.data.model.CancelOrderResponse
import ir.codecrafter.plasticproducts.data.model.EditOrderItemRequest
import ir.codecrafter.plasticproducts.data.model.EditOrderItemsRequest
import ir.codecrafter.plasticproducts.data.model.Invoice
import ir.codecrafter.plasticproducts.data.model.Order
import ir.codecrafter.plasticproducts.data.model.OrderCreateResponse
import ir.codecrafter.plasticproducts.data.model.OrderStatusHistoryEntry
import ir.codecrafter.plasticproducts.data.network.ApiEnvelope
import ir.codecrafter.plasticproducts.data.network.ApiError
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.network.OrderApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Reuses AuthRepository's AuthResult<T> — see ProductRepository for the same choice. */
@Singleton
class OrderRepository @Inject constructor(
    private val orderApi: OrderApi,
    private val json: Json,
) {

    suspend fun createOrder(): AuthResult<OrderCreateResponse> = safeCall { orderApi.createOrder() }

    suspend fun getOrderDetail(orderId: Int): AuthResult<Order> = safeCall { orderApi.getOrder(orderId) }

    suspend fun getStatusHistory(orderId: Int): AuthResult<List<OrderStatusHistoryEntry>> =
        safeCall { orderApi.getStatusHistory(orderId) }

    suspend fun cancelOrder(orderId: Int): AuthResult<CancelOrderResponse> =
        safeCall { orderApi.cancelOrder(orderId) }

    /**
     * Uses PATCH, not PUT — orders/views.py edit_items() handles both verbs
     * identically (it's a custom action, not a ModelViewSet partial update),
     * so there's no behavioral difference; PATCH is exposed here to match
     * the convention already used in UserApi.updateUser.
     */
    suspend fun editItems(orderId: Int, items: List<EditOrderItemRequest>): AuthResult<Order> =
        safeCall { orderApi.patchEditItems(orderId, EditOrderItemsRequest(items)) }

    suspend fun getInvoice(orderId: Int): AuthResult<Invoice> = safeCall { orderApi.getInvoice(orderId) }

    /**
     * Returns the raw PDF body for the caller to stream to a file — writing it to
     * disk is a UI-layer concern for a later task, not this repository's job.
     * Doesn't use safeCall(): invoice_pdf's success response isn't an ApiEnvelope
     * (it's raw PDF bytes), only its error paths are — see OrderApi.getInvoicePdf.
     */
    suspend fun downloadInvoicePdf(orderId: Int): AuthResult<ResponseBody> {
        return try {
            val response = orderApi.getInvoicePdf(orderId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    AuthResult.Success(body)
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
