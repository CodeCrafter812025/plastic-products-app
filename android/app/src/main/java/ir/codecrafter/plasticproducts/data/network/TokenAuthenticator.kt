package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.BuildConfig
import ir.codecrafter.plasticproducts.data.local.TokenManager
import ir.codecrafter.plasticproducts.data.model.RefreshTokenBody
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles 401s from the main OkHttpClient by refreshing the access token and
 * retrying the original request. Deliberately builds its own plain Retrofit/AuthApi
 * from a bare OkHttpClient (no auth interceptor, no authenticator) instead of being
 * handed the app's main Retrofit/AuthApi instance: those are built from the very
 * OkHttpClient this class is attached to as its authenticator, so depending on them
 * here would be a circular dependency (and would risk the refresh call itself
 * re-triggering this authenticator on a 401).
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    json: Json,
) : Authenticator {

    private val refreshApi: AuthApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AuthApi::class.java)

    private val refreshMutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Already retried once for this request; don't loop forever on a bad refresh.
        if (responseCount(response) >= 2) return null

        val failedAccessToken = tokenManager.accessToken

        val newAccessToken = runBlocking {
            refreshMutex.withLock {
                // Another request may have already refreshed while we waited for the
                // lock; only hit the network if our token is still the stale one.
                if (tokenManager.accessToken != failedAccessToken) {
                    tokenManager.accessToken
                } else {
                    refreshAccessToken()
                }
            }
        } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private suspend fun refreshAccessToken(): String? {
        val refreshToken = tokenManager.refreshToken ?: return null
        return try {
            val response = refreshApi.refreshToken(body = RefreshTokenBody(refresh = refreshToken))
            val envelope = response.body()
            val tokenPair = envelope?.data
            if (response.isSuccessful && envelope?.success == true && tokenPair != null) {
                tokenManager.accessToken = tokenPair.accessToken
                if (tokenPair.refreshToken != null) {
                    tokenManager.refreshToken = tokenPair.refreshToken
                }
                tokenPair.accessToken
            } else {
                // Phase 1: just drop the session. Redirecting to login is a UI-layer concern.
                tokenManager.clear()
                null
            }
        } catch (e: Exception) {
            tokenManager.clear()
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
