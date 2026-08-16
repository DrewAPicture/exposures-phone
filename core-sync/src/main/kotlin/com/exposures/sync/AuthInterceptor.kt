package com.exposures.sync

import okhttp3.Interceptor
import okhttp3.Response

/** Attaches [AuthProvider.authHeader] to every request, if present. */
class AuthInterceptor(private val authProvider: AuthProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val header = authProvider.authHeader()
        val request = if (header != null) {
            chain.request().newBuilder().header("Authorization", header).build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
