package com.ncflix.app.di

import com.ncflix.app.utils.AdBlockInterceptor
import com.ncflix.app.utils.Constants
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClient {

    // Shared OkHttpClient instance with Ad Blocking and connection pooling
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AdBlockInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Emulate a real browser to avoid basic blocking
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", Constants.USER_AGENT)
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}