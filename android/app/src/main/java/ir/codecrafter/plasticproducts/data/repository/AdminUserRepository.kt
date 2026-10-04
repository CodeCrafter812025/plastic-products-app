package ir.codecrafter.plasticproducts.data.repository

import ir.codecrafter.plasticproducts.data.model.AdminUser
import ir.codecrafter.plasticproducts.data.model.ToggleUserActiveResponse
import ir.codecrafter.plasticproducts.data.model.VisitorCreateBody
import ir.codecrafter.plasticproducts.data.model.VisitorPerformance
import ir.codecrafter.plasticproducts.data.network.AdminUserApi
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
class AdminUserRepository @Inject constructor(
    private val adminUserApi: AdminUserApi,
    private val json: Json,
) {

    /**
     * users/ has no server-side role filter (see AdminUserApi.getUsers's
     * KDoc) — this fetches the full user list and filters to roleFilter here,
     * client-side, same pattern as AdminProductRepository.getPriceHistory/
     * getStockHistory. roleFilter == null (the default) returns the
     * unfiltered list.
     */
    suspend fun getUsers(roleFilter: String? = null): AuthResult<List<AdminUser>> {
        return when (val result = safeCall { adminUserApi.getUsers() }) {
            is AuthResult.Success -> AuthResult.Success(
                if (roleFilter == null) result.data else result.data.filter { it.role == roleFilter },
            )
            else -> result
        }
    }

    suspend fun toggleUserActive(userId: Int): AuthResult<ToggleUserActiveResponse> =
        safeCall { adminUserApi.toggleUserActive(userId) }

    /**
     * A duplicate phone (or any other server-side validation failure) comes
     * back through the normal error envelope like any other failure —
     * result.message is already ready to display as-is, no special-casing
     * needed here for that case.
     */
    suspend fun createVisitor(phone: String, fullName: String): AuthResult<AdminUser> =
        safeCall { adminUserApi.createVisitor(VisitorCreateBody(phone = phone, fullName = fullName)) }

    suspend fun getVisitorPerformance(): AuthResult<List<VisitorPerformance>> =
        safeCall { adminUserApi.getVisitorPerformance() }

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
