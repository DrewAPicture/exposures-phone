package com.exposures.sync

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object SyncApiFactory {
    private val json = Json { ignoreUnknownKeys = true }

    fun create(baseUrl: String, authProvider: AuthProvider = NoOpAuthProvider): SyncApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authProvider))
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SyncApi::class.java)
    }
}
