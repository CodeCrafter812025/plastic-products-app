package ir.codecrafter.plasticproducts.data.repository

import ir.codecrafter.plasticproducts.data.model.AssignOrderRequest
import ir.codecrafter.plasticproducts.data.model.CancelOrderResponse
import ir.codecrafter.plasticproducts.data.model.Order
import ir.codecrafter.plasticproducts.data.model.OrderAssignment
import ir.codecrafter.plasticproducts.data.network.AdminOrderApi
import ir.codecrafter.plasticproducts.data.network.ApiEnvelope
import ir.codecrafter.plasticproducts.data.network.ApiError
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Reuses AuthRepository's AuthResult<T> — see ProductRepository for the same choice. */
@Singleton
class AdminOrderRepository @Inject constructor(
    private val adminOrderApi: AdminOrderApi,
    private val orderRepository: OrderRepository,
    private val json: Json,
) {

    /**
     * orders/ has no server-side status filter — OrderViewSet.get_queryset()
     * already returns every order for an admin caller (see
     * OrderRepository.getOrders's KDoc), so this reuses that method directly
     * instead of duplicating it against AdminOrderApi, and filters to
     * statusFilter here, client-side — same pattern as
     * AdminProductRepository.getPriceHistory/AdminUserRepository.getUsers.
     * statusFilter == null (the default) returns the unfiltered list.
     */
    suspend fun getOrders(statusFilter: String? = null): AuthResult<List<Order>> {
        return when (val result = orderRepository.getOrders()) {
            is AuthResult.Success -> AuthResult.Success(
                if (statusFilter == null) result.data else result.data.filter { it.status == statusFilter },
            )
            else -> result
        }
    }

    suspend fun assignOrder(orderId: Int, visitorId: Int, reason: String?): AuthResult<OrderAssignment> =
        safeCall {
            adminOrderApi.assignOrder(
                AssignOrderRequest(orderId = orderId, newVisitorId = visitorId, reason = reason),
            )
        }

    suspend fun cancelOrderAdmin(orderId: Int): AuthResult<CancelOrderResponse> =
        safeCall { adminOrderApi.cancelOrderAdmin(orderId) }

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
