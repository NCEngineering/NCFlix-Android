package com.ncflix.app

import com.ncflix.app.utils.AdBlocker
import org.junit.Test
import org.junit.Assert.*
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

class AdBlockerTest {

    @Test
    fun testIsAdHost() {
        // Mixed case input
        assertTrue("Should detect mixed case ad host", AdBlocker.isAdHost("www.GoogleAds.com"))
        assertTrue("Should detect doubleclick", AdBlocker.isAdHost("stats.doubleclick.net"))

        // Clean hosts
        assertFalse("Should not block clean host", AdBlocker.isAdHost("google.com"))
        assertFalse("Should not block example.com", AdBlocker.isAdHost("example.com"))
    }

    @Test
    fun testIsAdHttpUrl() {
        val adUrl = "https://www.googleads.com/some/path".toHttpUrl()
        assertTrue("Should detect ad url", AdBlocker.isAd(adUrl))

        val cleanUrl = "https://google.com/search".toHttpUrl()
        assertFalse("Should not block clean url", AdBlocker.isAd(cleanUrl))
    }

    @Test
    fun testIsAdString() {
         // Using isAd(String) which parses URI
         assertTrue(AdBlocker.isAd("https://www.GoogleAds.com"))
         assertFalse(AdBlocker.isAd("https://google.com"))
    }
}
