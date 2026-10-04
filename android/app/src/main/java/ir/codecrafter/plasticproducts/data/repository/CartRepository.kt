package ir.codecrafter.plasticproducts.data.repository

import ir.codecrafter.plasticproducts.data.model.Cart
import ir.codecrafter.plasticproducts.data.model.CartItem
import ir.codecrafter.plasticproducts.data.model.CartItemRequest
import ir.codecrafter.plasticproducts.data.network.ApiEnvelope
import ir.codecrafter.plasticproducts.data.network.ApiError
import ir.codecrafter.plasticproducts.data.network.CartApi
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Reuses AuthRepository's AuthResult<T> — see ProductRepository for the same choice. */
@Singleton
class CartRepository @Inject constructor(
    private val cartApi: CartApi,
    private val json: Json,
) {

    suspend fun getCart(): AuthResult<Cart> = safeCall { cartApi.getCart() }

    suspend fun addItem(productId: Int, quantity: String): AuthResult<CartItem> =
        safeCall { cartApi.addCartItem(CartItemRequest(productId = productId, quantity = quantity)) }

    suspend fun updateItem(id: Int, productId: Int, quantity: String): AuthResult<CartItem> =
        safeCall { cartApi.updateCartItem(id, CartItemRequest(productId = productId, quantity = quantity)) }

    suspend fun deleteItem(id: Int): AuthResult<Unit> = safeCallNoContent { cartApi.deleteCartItem(id) }

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

    /** DELETE /cart/{id}/ returns 204 No Content on success — no envelope body to decode. */
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
