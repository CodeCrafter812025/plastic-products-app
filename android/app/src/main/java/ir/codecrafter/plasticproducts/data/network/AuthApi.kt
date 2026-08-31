package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.data.model.AuthResponse
import ir.codecrafter.plasticproducts.data.model.OtpRequestBody
import ir.codecrafter.plasticproducts.data.model.OtpRequestResponse
import ir.codecrafter.plasticproducts.data.model.OtpVerifyBody
import ir.codecrafter.plasticproducts.data.model.RefreshTokenBody
import ir.codecrafter.plasticproducts.data.model.TokenPair
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface AuthApi {

    @POST("auth/otp/request/")
    suspend fun requestOtp(@Body body: OtpRequestBody): Response<ApiEnvelope<OtpRequestResponse>>

    @POST("auth/otp/verify/")
    suspend fun verifyOtp(@Body body: OtpVerifyBody): Response<ApiEnvelope<AuthResponse>>

    /**
     * TokenRefreshView is registered directly at /api/token/refresh/ in
     * plastic_products/urls.py, outside the /api/v1/ router. BuildConfig.BASE_URL
     * ends in "/api/v1/", so a relative path here would resolve to the wrong place
     * (".../api/v1/api/token/refresh/"). The leading "/" in the @Url default makes
     * Retrofit/OkHttp resolve it against the base URL's scheme+host+port only,
     * bypassing the "/api/v1/" path segment entirely.
     */
    @POST
    suspend fun refreshToken(
        @Url url: String = "/api/token/refresh/",
        @Body body: RefreshTokenBody,
    ): Response<ApiEnvelope<TokenPair>>
}
