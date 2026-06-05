package com.example.rm365warehousesoftware.data.remote

import android.content.Context
import androidx.core.content.edit

/**
 * Thread-safe holder for the JWT used to authenticate API requests.
 *
 * The token is persisted in SharedPreferences so it survives process death,
 * and cached in memory for fast access from the OkHttp interceptor.
 */
class TokenProvider(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cachedToken: String? = prefs.getString(KEY_TOKEN, null)

    /** Returns the current JWT, or null if the user is not authenticated. */
    fun getToken(): String? = cachedToken

    /** Saves a new JWT (call this after a successful login). */
    fun saveToken(token: String) {
        cachedToken = token
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    /** Clears the stored JWT (call this on logout or a 401 response). */
    fun clearToken() {
        cachedToken = null
        prefs.edit { remove(KEY_TOKEN) }
    }

    private companion object {
        const val PREFS_NAME = "rm365_auth_prefs"
        const val KEY_TOKEN = "jwt_token"
    }
}
