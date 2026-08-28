package ir.codecrafter.plasticproducts.data.repository

import ir.codecrafter.plasticproducts.data.local.TokenManager
import ir.codecrafter.plasticproducts.data.model.UpdateProfileBody
import ir.codecrafter.plasticproducts.data.model.UserProfile
import ir.codecrafter.plasticproducts.data.network.ApiEnvelope
import ir.codecrafter.plasticproducts.data.network.ApiError
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.network.UserApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reuses AuthRepository's AuthResult<T> rather than a near-identical
 * ProfileResult<T> — same Success/RateLimited/Error/NetworkError shape applies
 * to any API call, not just auth ones.
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val userApi: UserApi,
    private val tokenManager: TokenManager,
    private val json: Json,
) {

    suspend fun getProfile(): AuthResult<UserProfile> {
        val userId = tokenManager.userId
            ?: return AuthResult.Error(code = "NO_SESSION", message = null)
        return safeCall { userApi.getUser(userId) }
    }

    suspend fun updateProfile(fullName: String, address: String?): AuthResult<UserProfile> {
        val userId = tokenManager.userId
            ?: return AuthResult.Error(code = "NO_SESSION", message = null)
        return safeCall { userApi.updateUser(userId, UpdateProfileBody(fullName = fullName, address = address)) }
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
