package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkConfig {
    /**
     * User-Agent configurable utilizado en todas las peticiones (API, imágenes con Coil y streams de Media3).
     * Los servidores Xtream Codes bloquean el User-Agent por defecto "okhttp".
     */
    const val USER_AGENT: String = "VLC/3.0.20 LibVLC/3.0.20"

    const val CONNECT_TIMEOUT_SECONDS: Long = 15L
    const val READ_TIMEOUT_SECONDS: Long = 30L
    const val WRITE_TIMEOUT_SECONDS: Long = 30L
}

class UserAgentInterceptor(
    private val userAgent: String = NetworkConfig.USER_AGENT
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestWithUserAgent = original.newBuilder()
            .header("User-Agent", userAgent)
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}

class RetryInterceptor(
    private val maxRetries: Int = 1
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        var lastException: IOException? = null

        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(request)
                // Si la respuesta fue exitosa o es un error del cliente 4xx (excepto 408 Request Timeout), no reintentar
                if (response.isSuccessful || (response.code in 400..499 && response.code != 408)) {
                    return response
                }
                // Si fue un error transitorio de servidor (5xx) o 408 y aún queda reintento
                if (attempt < maxRetries) {
                    response.close()
                    attempt++
                    continue
                }
                return response
            } catch (e: IOException) {
                lastException = e
                if (attempt >= maxRetries) {
                    throw e
                }
                attempt++
            }
        }
        throw lastException ?: IOException("Error de conexión desconocido")
    }
}

object ApiClient {

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(UserAgentInterceptor(NetworkConfig.USER_AGENT))
            .addInterceptor(RetryInterceptor(maxRetries = 1))
            .addInterceptor(logging)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://sdmx.vip:80/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
    }

    val omdbService: OmdbApiService by lazy {
        retrofit.create(OmdbApiService::class.java)
    }
}
