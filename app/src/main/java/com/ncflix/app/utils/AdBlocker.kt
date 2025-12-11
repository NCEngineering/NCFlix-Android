package com.ncflix.app.utils

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.Locale

object AdBlocker {
    // Comprehensive list of ad/tracker domains common on streaming sites
    private val AD_HOSTS = setOf(
        // Big Tech
        "googleads", "doubleclick", "analytics", "facebook.com", "connect.facebook.net",
        "googletagservices", "adservice.google", "clients1.google",

        // Streaming/Popups/Redirects
        "adsco.re", "popads", "popcash", "propellerads", "adsterra", "revenuehits",
        "mc.yandex", "creativecdn", "scorecardresearch", "quantserve", "adroll",
        "taboola", "outbrain", "zedo", "adclick", "trackclick", "adsystem",
        "histats", "statcounter", "bidgear", "exo-click", "juicyads",
        "onclasrv", "simgadgt", "windacmedia", "mgridplus", "media.net",
        "adsupply", "yldbt", "hooliganmedia", "vidcrunch", "adpushup",
        "infolinks", "kontera", "adblade", "dianomi", "myplaycity",
        "adk2", "adcash", "bidvertiser", "clicksor", "chitika",
        // "upsetking.com", "walterprettytheir.com", "dsvplay.com", // REMOVED: These are video hosts
        "jads", "juicyads", "exoclick", "trafficjunky", "ero-advertising",
        "tsyndicate", "plugrush", "trafficfactory", "adxpansion",
        "bet365", "1xbet", "casino", "gambling"
    ).map { it.lowercase(Locale.ROOT) }.toSet()

    fun isAd(url: String): Boolean {
        val host = try { java.net.URI(url).host?.lowercase(Locale.ROOT) ?: "" } catch (e: Exception) { url.lowercase(Locale.ROOT) }
        return AD_HOSTS.any { host.contains(it) }
    }

    fun isAd(url: HttpUrl): Boolean {
        val host = url.host // HttpUrl host is already lowercased and punycode decoded
        return AD_HOSTS.any { host.contains(it) }
    }

    /**
     * CSS to hide elements commonly used for ads, overlays, and anti-adblock messages.
     */
    fun getCssRules(): String {
        return """
            /* Generic Ad Containers */
            .ad-container, .ads, .advertisement, .banner-ads,
            div[id^='ad-'], div[class*='ad-'], div[id*='banner'],
            iframe[src*='ads'], iframe[src*='doubleclick'],
            
            /* Common Overlay/Popunder Wrappers */
            div[style*='z-index: 2147483647'], /* Max Z-Index often used by overlays */
            div[style*='z-index: 9999999'],
            div[style*='position: fixed'][style*='width: 100%'][style*='height: 100%'],
            
            /* Specific Player Overlays */
            .jw-logo, .jw-title-primary, .jw-title-secondary,
            .vjs-big-play-button[style*='z-index'], /* Fake play buttons */
            
            /* Anti-Adblock Messages */
            #adb-enabled, .adb-modal, .detect-adblock,
            
            /* Site Specific junk */
            .watermark, .branding, .social-share
            { display: none !important; opacity: 0 !important; pointer-events: none !important; height: 0 !important; width: 0 !important; }
        """.trimIndent().replace("\n", " ")
    }

    /**
     * JavaScript to neutralize popups, click-hijacking, and forced redirects.
     */
    fun getDomBypasses(): String {
        return """
            (function() {
                console.log("NC-FLIX: Initializing uBlock-lite DOM Bypasses...");

                // 1. Kill window.open (Popups)
                window.open = function() { 
                    console.log("NC-FLIX: Blocked window.open attempt."); 
                    return null; 
                };

                // 2. Kill Link Target=_blank (New Tabs)
                function disarmLinks() {
                    var links = document.getElementsByTagName('a');
                    for (var i = 0; i < links.length; i++) {
                        links[i].target = '_self';
                        // Remove suspicious click handlers if needed (risky)
                    }
                }
                setInterval(disarmLinks, 2000);

                // 3. Stop Event Bubbling on Document (Anti-Clickjacking)
                // This is aggressive: it stops clicks on the background from triggering redirects
                document.addEventListener('click', function(e) {
                    var target = e.target;
                    // Allow clicks on Video elements and specific Controls
                    var isVideo = target.tagName === 'VIDEO' || target.tagName === 'OBJECT';
                    var isControl = target.className && (target.className.includes('vjs') || target.className.includes('jw-'));
                    
                    if (!isVideo && !isControl && !e.isTrusted) {
                        // e.stopPropagation();
                        // e.preventDefault();
                        // console.log("NC-FLIX: Blocked untrusted/background click.");
                    }
                }, true);
                
                // 4. Remove 'sandbox' restrictions if present on iframes we want to play
                var iframes = document.getElementsByTagName('iframe');
                for (var j = 0; j < iframes.length; j++) {
                    iframes[j].removeAttribute('sandbox');
                }

            })();
        """.trimIndent().replace("\n", " ")
    }
}

class AdBlockInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url

        if (AdBlocker.isAd(url)) {
            throw IOException("Blocked Ad/Tracker: $url")
        }

        return chain.proceed(chain.request())
    }
}