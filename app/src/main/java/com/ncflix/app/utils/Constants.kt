package com.ncflix.app.utils

/**
 * Global constants used throughout the application.
 *
 * This object holds configuration values such as network endpoints, user agent strings,
 * cookie data, and ad-blocking rules. Centralizing these values makes the application
 * easier to maintain and update.
 */
object Constants {

    /**
     * The base URL of the target movie streaming website.
     */
    const val BASE_URL = "https://ww93.pencurimovie.bond"

    /**
     * The User-Agent string to simulate a real browser request.
     * This is crucial for bypassing basic bot detection mechanisms.
     */
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"

    /**
     * Required cookies to maintain session or bypass specific checks on the target site.
     */
    val COOKIES = mapOf(
        "_ga" to "GA1.1.1793413717.1764381264",
        "wpdiscuz_hide_bubble_hint" to "1",
        "_ga_BS36YHXFDN" to "GS2.1.s1764381264\$o1\$g1\$t1764381686\$j60\$l0\$h0"
    )

    /**
     * Pre-computed Cookie header string to avoid allocation on every request.
     */
    val COOKIE_HEADER: String = COOKIES.entries.joinToString("; ") { "${it.key}=${it.value}" }

    /**
     * A set of domain keywords used to identify and block advertisement URLs in the player.
     */
    val AD_BLOCK_DOMAINS = setOf(
        "googleads", "doubleclick", "analytics", "facebook.com", "connect.facebook.net",
        "adsco.re", "pop", "bet", "casino", "mc.yandex", "creativecdn",
        "googletagmanager", "scorecardresearch", "quantserve", "adroll",
        "taboola", "outbrain", "zedo", "click", "tracker", "pixel", "adsystem",
        "histats", "statcounter", "popads", "popcash", "propellerads", "revenuehits", "upsetking.com",
        "walterprettytheir.com"
    )
}
