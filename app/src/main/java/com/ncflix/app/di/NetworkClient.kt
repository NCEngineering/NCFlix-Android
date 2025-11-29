package com.ncflix.app.di

import com.ncflix.app.utils.AdBlockInterceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClient {

    // Shared OkHttpClient instance with Ad Blocking and connection pooling
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AdBlockInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            // Emulate a real browser to avoid basic blocking
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .method(original.method, original.body)
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}