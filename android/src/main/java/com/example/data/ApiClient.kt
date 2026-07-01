package com.example.data

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://ryd-api.ocaya.space"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    @Volatile
    private var cachedToken: String? = null

    private fun fetchFirebaseToken(forceRefresh: Boolean = false): String? {
        return try {
            val task = FirebaseAuth.getInstance().currentUser
                ?.getIdToken(forceRefresh)
                ?: Tasks.forResult(null)
            Tasks.await(task)?.token
        } catch (e: Exception) { null }
    }

    private val authInterceptor = Interceptor { chain ->
        var token = cachedToken
        if (token == null) {
            token = fetchFirebaseToken(false)
            cachedToken = token
        }
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val authenticator = okhttp3.Authenticator { _, response ->
        if (response.code == 401 || response.code == 403) {
            val freshToken = fetchFirebaseToken(forceRefresh = true)
            if (freshToken != null) {
                cachedToken = freshToken
                response.request.newBuilder()
                    .header("Authorization", "Bearer $freshToken")
                    .build()
            } else null
        } else null
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .authenticator(authenticator)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: BodaApiService = retrofit.create(BodaApiService::class.java)

    fun getBaseUrl(): String = BASE_URL

    fun getWebSocketUrl(): String = BASE_URL

    fun invalidateToken() {
        cachedToken = null
    }
}
