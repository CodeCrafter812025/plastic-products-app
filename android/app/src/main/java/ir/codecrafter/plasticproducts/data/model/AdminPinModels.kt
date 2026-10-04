package ir.codecrafter.plasticproducts.data.model

import kotlinx.serialization.Serializable

/** Body for POST auth/verify-admin-pin/ — the second factor after verify_otp returns requires_pin=true. */
@Serializable
data class AdminPinVerifyBody(
    val phone: String,
    val pin: String,
)

/** Body for POST users/set-admin-pin/ — lets the logged-in admin set/change their own PIN. */
@Serializable
data class SetAdminPinBody(
    val pin: String,
)

/** Mirrors the dict returned by UserViewSet.set_admin_pin() in backend/users/views.py: {'message': ...}. */
@Serializable
data class SetAdminPinResponse(
    val message: String,
)
