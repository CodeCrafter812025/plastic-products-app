package ir.codecrafter.plasticproducts.data.repository

import ir.codecrafter.plasticproducts.data.local.TokenManager
import ir.codecrafter.plasticproducts.data.model.AuthResponse
import ir.codecrafter.plasticproducts.data.model.OtpRequestBody
import ir.codecrafter.plasticproducts.data.model.OtpRequestResponse
import ir.codecrafter.plasticproducts.data.model.OtpVerifyBody
import ir.codecrafter.plasticproducts.data.network.ApiEnvelope
import ir.codecrafter.plasticproducts.data.network.ApiError
import ir.codecrafter.plasticproducts.data.network.AuthApi
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()

    /**
     * HTTP 429 from an auth endpoint. Backed by OTPRequestThrottle (3/hour per IP)
     * and OTPPhoneThrottle (3/hour per phone) on auth/otp/request/ (see
     * backend/users/throttles.py) — kept separate from Error so the UI layer can
     * show a distinct "try again later" message without inspecting error codes.
     */
    data class RateLimited(val message: String?) : AuthResult<Nothing>()

    data class Error(val code: String, val message: ErrorMessage?) : AuthResult<Nothing>()

    data object NetworkError : AuthResult<Nothing>()
}

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val json: Json,
) {

    suspend fun requestOtp(phone: String, purpose: String): AuthResult<OtpRequestResponse> =
        safeCall { authApi.requestOtp(OtpRequestBody(phone = phone, purpose = purpose)) }

    suspend fun verifyOtp(
        phone: String,
        code: String,
        purpose: String,
        fullName: String? = null,
    ): AuthResult<AuthResponse> {
        val result = safeCall {
            authApi.verifyOtp(OtpVerifyBody(phone = phone, code = code, purpose = purpose, fullName = fullName))
        }
        if (result is AuthResult.Success) {
            val auth = result.data
            tokenManager.saveSession(
                accessToken = auth.token,
                refreshToken = auth.refreshToken,
                userId = auth.user.id,
                role = auth.user.role,
            )
        }
        return result
    }

    fun logout() {
        tokenManager.clear()
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

    /**
     * Error responses never have their "data" payload parsed (it's absent on error
     * anyway), so JsonElement stands in for the generic T just to read out .error.
     * Catches broadly (not just SerializationException) because ErrorMessageSerializer
     * throws a plain IllegalArgumentException for an error.message shape it doesn't
     * model (e.g. the bare JSON array the backend sends for non_field_errors) — this
     * is a network response boundary, so a body we can't parse must degrade to a
     * generic error rather than crash the caller.
     */
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
