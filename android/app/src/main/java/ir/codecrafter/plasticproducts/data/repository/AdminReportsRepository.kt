package ir.codecrafter.plasticproducts.data.repository

import ir.codecrafter.plasticproducts.data.model.LowStockProduct
import ir.codecrafter.plasticproducts.data.model.RevenueReport
import ir.codecrafter.plasticproducts.data.model.SignupCount
import ir.codecrafter.plasticproducts.data.model.TopProduct
import ir.codecrafter.plasticproducts.data.network.AdminReportsApi
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
class AdminReportsRepository @Inject constructor(
    private val adminReportsApi: AdminReportsApi,
    private val json: Json,
) {

    suspend fun getOrderCounts(): AuthResult<Map<String, Int>> = safeCall { adminReportsApi.getOrderCounts() }

    /** from/to must already be in YYYY-MM-DD format by the time they reach here — the server 400s otherwise. */
    suspend fun getRevenue(from: String, to: String): AuthResult<RevenueReport> =
        safeCall { adminReportsApi.getRevenue(from, to) }

    suspend fun getTopProducts(limit: Int? = null): AuthResult<List<TopProduct>> =
        safeCall { adminReportsApi.getTopProducts(limit) }

    suspend fun getLowStock(threshold: Int? = null): AuthResult<List<LowStockProduct>> =
        safeCall { adminReportsApi.getLowStock(threshold) }

    suspend fun getSignups(period: String? = null): AuthResult<List<SignupCount>> =
        safeCall { adminReportsApi.getSignups(period) }

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
