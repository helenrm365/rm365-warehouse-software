package com.example.rm365warehousesoftware.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that attaches the JWT Bearer token to every outgoing request.
 *
 * If no token is available, the request is sent unmodified (e.g. the login call).
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenProvider.getToken()

        // No token yet (e.g. login endpoint) -> proceed without the header.
        if (token.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
