package com.ncflix.app

import org.junit.Test
import org.junit.Assert.*

class PlayerSnifferTest {

    private fun isVideoUrl_Original(urlString: String): Boolean {
        val lowerUrl = urlString.lowercase()
        if (lowerUrl.contains(".mp4") || lowerUrl.contains(".m3u8") || lowerUrl.contains(".mkv") || lowerUrl.contains(".ts")) {
             // Filter out small segments if possible, but for now capture all potential streams
             if (!lowerUrl.contains("favicon") && !lowerUrl.contains(".png")) {
                 return true
             }
        }
        return false
    }

    private fun isVideoUrl_Optimized(urlString: String): Boolean {
        // Optimization: avoid allocation of lowercased string
        if (urlString.contains(".mp4", ignoreCase = true) ||
            urlString.contains(".m3u8", ignoreCase = true) ||
            urlString.contains(".mkv", ignoreCase = true) ||
            urlString.contains(".ts", ignoreCase = true)) {

             // Filter out small segments if possible, but for now capture all potential streams
             if (!urlString.contains("favicon", ignoreCase = true) && !urlString.contains(".png", ignoreCase = true)) {
                 return true
             }
        }
        return false
    }

    @Test
    fun testLogicEquivalence() {
        val cases = listOf(
            "https://example.com/video.mp4" to true,
            "https://example.com/VIDEO.MP4" to true,
            "https://example.com/video.m3u8?token=123" to true,
            "https://example.com/segment.ts" to true,
            "https://example.com/image.png" to false,
            "https://example.com/favicon.ico" to false, // Original logic doesn't block .ico but blocks 'favicon' string
            "https://example.com/favicon.png" to false,
            "https://example.com/video.mp4/favicon" to false, // Original logic blocks if 'favicon' is present
            "https://example.com/data.json" to false,
            "HTTPS://SERVER.COM/MOVIE.MKV" to true
        )

        for ((url, expected) in cases) {
            val original = isVideoUrl_Original(url)
            val optimized = isVideoUrl_Optimized(url)

            assertEquals("Original logic failed for $url", expected, original)
            assertEquals("Optimized logic mismatch for $url", original, optimized)
        }
    }
}
