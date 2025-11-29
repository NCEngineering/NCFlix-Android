package com.ncflix.app.utils

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

object AdBlocker {
    // A basic list of common ad/tracker domains to block during scraping/playback
    private val AD_HOSTS = setOf(
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "www.google-analytics.com",
        "tpc.googlesyndication.com",
        "syndication.twitter.com",
        "ads.twitter.com",
        "static.ads-twitter.com",
        "www.facebook.com",
        "connect.facebook.net",
        "creative.ak.fbcdn.net",
        "www.googletagservices.com",
        "adservice.google.com",
        "clients1.google.com",
        "ad.doubleclick.net"
        // Add more known ad hosts here as needed
    )

    fun isAd(host: String): Boolean {
        return AD_HOSTS.any { host.contains(it, ignoreCase = true) }
    }
}

class AdBlockInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url
        val host = url.host

        if (AdBlocker.isAd(host)) {
            throw IOException("Blocked Ad/Tracker: $host")
        }

        return chain.proceed(chain.request())
    }
}