package ir.codecrafter.plasticproducts.data.network

import ir.codecrafter.plasticproducts.data.model.UpdateProfileBody
import ir.codecrafter.plasticproducts.data.model.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface UserApi {

    @GET("users/{id}/")
    suspend fun getUser(@Path("id") id: Int): Response<ApiEnvelope<UserProfile>>

    /**
     * UserViewSet is a full ModelViewSet, so PUT (update) works too — PATCH is used
     * here because it only requires the fields actually being changed. A PUT would
     * also need "phone" in the body since it's required (non-blank) on the model
     * and not in the serializer's read_only_fields.
     */
    @PATCH("users/{id}/")
    suspend fun updateUser(
        @Path("id") id: Int,
        @Body body: UpdateProfileBody,
    ): Response<ApiEnvelope<UserProfile>>
}
