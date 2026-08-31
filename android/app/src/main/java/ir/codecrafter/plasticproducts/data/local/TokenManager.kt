package ir.codecrafter.plasticproducts.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()
        }

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) {
            prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()
        }

    var userId: Int?
        get() = prefs.getInt(KEY_USER_ID, -1).takeIf { it != -1 }
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_USER_ID) else putInt(KEY_USER_ID, value)
            }.apply()
        }

    var role: String?
        get() = prefs.getString(KEY_ROLE, null)
        set(value) {
            prefs.edit().putString(KEY_ROLE, value).apply()
        }

    /**
     * Stores a freshly authenticated session in one atomic write. refreshToken is
     * nullable because AuthViewSet.verify_otp() currently only ever returns an
     * access token — passing null here explicitly clears any refresh token left
     * over from a previous session instead of leaking it into the new one.
     */
    fun saveSession(accessToken: String, refreshToken: String?, userId: Int, role: String) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            if (refreshToken != null) putString(KEY_REFRESH_TOKEN, refreshToken) else remove(KEY_REFRESH_TOKEN)
            putInt(KEY_USER_ID, userId)
            putString(KEY_ROLE, role)
        }.apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_FILE_NAME = "auth_secure_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_ROLE = "role"
    }
}
