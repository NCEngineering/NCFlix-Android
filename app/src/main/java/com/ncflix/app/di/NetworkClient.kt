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
                val url = original.url.toString()
                
                val requestBuilder = original.newBuilder()
                    .header("User-Agent", Constants.USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")

                // Only add specific headers for the target site to avoid leaking cookies/referer to external sites (like IMDb)
                if (url.contains("pencurimovie")) {
                    val cookieHeader = Constants.COOKIES.entries.joinToString("; ") { "${it.key}=${it.value}" }
                    requestBuilder.header("Cookie", cookieHeader)
                    requestBuilder.header("Referer", Constants.BASE_URL)
                }
                
                chain.proceed(requestBuilder.build())
            }
            .build()
    }
}