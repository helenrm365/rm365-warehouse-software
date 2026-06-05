package com.example.rm365warehousesoftware.data.remote

import android.content.Context
import com.example.rm365warehousesoftware.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds and exposes a singleton [ApiService].
 *
 * Call [init] once (e.g. from your Application class) before using [apiService].
 */
object RetrofitClient {

    // TODO: point this at your real environment (must end with a trailing slash).
    private const val BASE_URL = "https://api.rm365.example.com/v1/"

    @Volatile
    private var service: ApiService? = null

    /** The configured API service. Throws if [init] has not been called. */
    val apiService: ApiService
        get() = service ?: error("RetrofitClient.init(context) must be called before use")

    fun init(context: Context) {
        if (service != null) return
        synchronized(this) {
            if (service != null) return

            val tokenProvider = TokenProvider(context)

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenProvider))
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            service = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
