package com.ncflix.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.Animation
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.lifecycleScope
import com.ncflix.app.R
import com.ncflix.app.data.MovieRepository
import com.ncflix.app.utils.Constants
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

/**
 * Activity for playing videos (movies or episodes).
 *
 * This activity hosts a WebView to play content from external streaming servers.
 * It handles server selection, ad blocking, custom gesture controls, and integration
 * with the web player via JavaScript injection.
 */
class PlayerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtStatus: TextView
    private lateinit var txtGestureFeedback: TextView
    private val repository = MovieRepository()

    private var serverList = ArrayList<String>()
    private var currentServerIndex = 0
    private var currentHost: String = ""

    private var isVideoPlaying = false
    private lateinit var gestureDetector: GestureDetectorCompat

    /**
     * Called when the activity is starting.
     *
     * Initializes the WebView and other UI components, hides the system UI for fullscreen playback,
     * sets up gesture detectors, and starts the server initialization process if an episode URL is provided.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in [onSaveInstanceState].  <b><i>Note: Otherwise it is null.</i></b>
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        txtStatus = findViewById(R.id.txtStatus)
        txtGestureFeedback = findViewById(R.id.txtGestureFeedback)

        hideSystemUI()
        setupWebPlayer()
        setupGestures()

        val episodeUrl = intent.getStringExtra("EPISODE_URL")
        if (episodeUrl != null) {
            initializeServers(episodeUrl)
        }
    }

    /**
     * Sets up gesture detection for video controls.
     *
     * Handles double taps for seeking (left for backward, right for forward) and play/pause toggling.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val screenWidth = webView.width
                val tapX = e.x

                if (tapX < screenWidth / 3) {
                    seekBackward()
                    showGestureFeedback("<< 10s")
                } else if (tapX > (screenWidth * 2) / 3) {
                    seekForward()
                    showGestureFeedback("10s >>")
                } else {
                    togglePlayPause()
                }
                return true
            }

            override fun onDown(e: MotionEvent): Boolean = true
        })

        webView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    /**
     * Displays a temporary visual feedback for gestures.
     *
     * @param text The text to display (e.g., "10s >>").
     */
    private fun showGestureFeedback(text: String) {
        txtGestureFeedback.text = text
        txtGestureFeedback.visibility = View.VISIBLE

        val animation = AnimationUtils.loadAnimation(this, R.anim.fade_in_out)

        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                txtGestureFeedback.visibility = View.GONE
            }
        })
        txtGestureFeedback.startAnimation(animation)
    }

    /**
     * Seeks forward in the video by 10 seconds via JavaScript injection.
     */
    private fun seekForward() {
        webView.evaluateJavascript("document.querySelector('video').currentTime += 10;", null)
    }

    /**
     * Seeks backward in the video by 10 seconds via JavaScript injection.
     */
    private fun seekBackward() {
        webView.evaluateJavascript("document.querySelector('video').currentTime -= 10;", null)
    }

    /**
     * Toggles play/pause state of the video via JavaScript injection.
     */
    private fun togglePlayPause() {
        webView.evaluateJavascript("""
            (function() {
                var v = document.querySelector('video');
                if (v) {
                    if (v.paused) {
                        v.play();
                        window.Android.onPlayPauseToggled('play');
                    } else {
                        v.pause();
                        window.Android.onPlayPauseToggled('pause');
                    }
                }
            })();
        """, null)
    }

    /**
     * Initializes the list of available video servers for the given episode URL.
     *
     * @param url The URL of the episode page.
     */
    private fun initializeServers(url: String) {
        lifecycleScope.launch {
            txtStatus.text = "Scanning Servers..."
            serverList = repository.extractAllServers(url)

            if (serverList.isNotEmpty()) {
                loadCurrentServer()
            } else {
                txtStatus.text = "Error: No servers found."
                progressBar.visibility = View.GONE
            }
        }
    }

    /**
     * Loads the current server URL into the WebView.
     *
     * Iterates through the [serverList]. If the index is out of bounds, it indicates all servers failed.
     */
    private fun loadCurrentServer() {
        if (currentServerIndex < serverList.size) {
            val url = serverList[currentServerIndex]
            try { currentHost = Uri.parse(url).host ?: "" } catch(e: Exception){}

            txtStatus.text = "Trying Server ${currentServerIndex + 1}..."
            txtStatus.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE

            println("NC-FLIX: Loading -> $url")
            val headers = mapOf("Referer" to "https://ww93.pencurimovie.bond/")
            webView.loadUrl(url, headers)
        } else {
            txtStatus.text = "Failed: All servers are dead."
            progressBar.visibility = View.GONE
        }
    }

    /**
     * Configures the WebView settings and clients.
     *
     * Enables JavaScript, DOM storage, and sets up custom [WebChromeClient] and [WebViewClient]
     * to handle content loading, ad blocking, and JavaScript injection.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebPlayer() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            allowFileAccess = true
            allowContentAccess = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = false
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean = false

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {}
            override fun onHideCustomView() {}
        }

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url != null && (url.contains(currentHost) || url.contains("dsvplay") || url.contains("myvidplay") || url.contains("voe.sx") || url.contains("walterprettytheir"))) {
                    return false
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                Handler(Looper.getMainLooper()).postDelayed({
                    injectCSS()
                    injectAutoPlay()
                    injectAdBlockOverrides()
                }, 1000)

                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isVideoPlaying) {
                        injectErrorDetector()
                    }
                }, 7000)

                injectResumeDetector()
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url.toString().lowercase()
                if (isAd(url)) {
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
    }

    /**
     * Injects JavaScript to override ad-related behaviors like window.open and untrusted clicks.
     */
    private fun injectAdBlockOverrides() {
        val js = """
            (function() {
                window.open = function() { console.log("NC-FLIX: Blocked window.open popup."); return null; };
                document.body.addEventListener('click', function(e) {
                    if (!e.isTrusted) { 
                        e.stopPropagation();
                        e.preventDefault();
                        console.log("NC-FLIX: Blocked untrusted click.");
                    }
                }, true);
            })();
        """.trimIndent().replace("\n", " ")
        webView.evaluateJavascript(js, null)
    }

    /**
     * Checks if a URL matches known ad domains.
     *
     * @param url The URL to check.
     * @return True if the URL is an ad, false otherwise.
     */
    private fun isAd(url: String): Boolean {
        for (domain in Constants.AD_BLOCK_DOMAINS) {
            if (url.contains(domain)) return true
        }
        return false
    }

    /**
     * Javascript Interface for communication between the WebView and the Android application.
     */
    inner class WebAppInterface(private val context: Context) {
        /**
         * Called when a playback error is detected via JavaScript.
         */
        @JavascriptInterface
        fun onErrorDetected() {
            runOnUiThread {
                if (!isVideoPlaying) {
                    println("NC-FLIX: Dead Link Confirmed by JS (Visual Check)! Switching...")
                    currentServerIndex++
                    loadCurrentServer()
                }
            }
        }

        /**
         * Called when the resume popup is detected via JavaScript.
         *
         * @param time The timestamp where playback stopped.
         */
        @JavascriptInterface
        fun onResumeDetected(time: String) {
            runOnUiThread { showResumeDialog(time) }
        }

        /**
         * Called when the video starts playing.
         */
        @JavascriptInterface
        fun onVideoStarted() {
            runOnUiThread {
                if (!isVideoPlaying) {
                    println("NC-FLIX: Video Started! Locking server.")
                    isVideoPlaying = true
                    progressBar.visibility = View.GONE
                    txtStatus.visibility = View.GONE
                }
            }
        }

        /**
         * Called when play/pause is toggled via JavaScript.
         *
         * @param state The new state ("play" or "pause").
         */
        @JavascriptInterface
        fun onPlayPauseToggled(state: String) {
            runOnUiThread {
                if (state == "play") {
                    showGestureFeedback("▶")
                } else if (state == "pause") {
                    showGestureFeedback("⏸")
                }
            }
        }
    }

    /**
     * Shows a dialog asking the user if they want to resume playback from a previous position.
     *
     * @param time The timestamp string.
     */
    private fun showResumeDialog(time: String) {
        AlertDialog.Builder(this)
            .setTitle("Resume Playing?")
            .setMessage("Left off at $time. Resume?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                webView.evaluateJavascript("document.querySelector('#yesplease').click();", null)
                Handler(Looper.getMainLooper()).postDelayed({ injectAutoPlay() }, 1000)
            }
            .setNegativeButton("No") { _, _ ->
                webView.evaluateJavascript("document.querySelector('#no_thanks').click();", null)
                Handler(Looper.getMainLooper()).postDelayed({ injectAutoPlay() }, 1000)
            }
            .show()
    }

    /**
     * Injects JavaScript to detect video playback errors (e.g., "Not Found").
     */
    private fun injectErrorDetector() {
        val js = """
            (function() {
                var errorImg = document.querySelector('img[src*="no_video"]');
                var h1 = document.querySelector('h1');
                var is404 = h1 && (h1.innerText.includes("Not Found") || h1.innerText.includes("File not found"));
                
                var isErrorVisible = false;
                
                if (errorImg && errorImg.offsetParent !== null) {
                    isErrorVisible = true;
                }
                
                if (h1 && h1.innerText.includes("Not Found") && h1.offsetParent !== null) {
                    isErrorVisible = true;
                }
                
                if (isErrorVisible) {
                    window.Android.onErrorDetected();
                }
                
                var video = document.querySelector('video');
                if (video) {
                    video.addEventListener('playing', function() { window.Android.onVideoStarted(); });
                }
            })();
        """.trimIndent().replace("\n", " ")
        webView.evaluateJavascript(js, null)
    }

    /**
     * Injects JavaScript to detect the resume playback popup.
     */
    private fun injectResumeDetector() {
        val hideResumePopupCss = "#checkresume_div_n { display: none !important; }"
        val injectHideJs = "var style = document.createElement('style'); style.innerHTML = '$hideResumePopupCss'; document.head.appendChild(style);"
        webView.evaluateJavascript(injectHideJs, null)

        val js = """
            (function() {
                var checkResume = setInterval(function() {
                    var resumeDiv = document.querySelector('#checkresume_div_n');
                    if (resumeDiv) {
                        var timeSpan = document.querySelector('#lefttime');
                        var time = timeSpan ? timeSpan.innerText : "Unknown";
                        window.Android.onResumeDetected(time);
                        clearInterval(checkResume);
                    }
                }, 500);
                setTimeout(function(){ clearInterval(checkResume); }, 5000); 
            })();
        """.trimIndent().replace("\n", " ")
        webView.evaluateJavascript(js, null)
    }

    /**
     * Injects CSS to hide controls, ads, and force fullscreen.
     */
    private fun injectCSS() {
        // HIDES ALL CONTROLS AND FORCES FULLSCREEN VIEWPORT
        val css = """
            #loading,.loading,.ad-container,.popup,.banner,#ads,.jw-logo,.watermark,.eruda-container{display:none !important;}
            body, html { background-color: black !important; margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden !important; }
            video { width: 100% !important; height: 100vh !important; object-fit: contain; }
            
            /* HIDE ALL CONTROLS FOR GESTURE CONTROL */
            .vjs-control-bar, .jw-controlbar { 
                opacity: 0 !important; 
                pointer-events: none !important; 
                display: none !important;
            } 
            
            .vjs-big-play-button { display: none !important; }
            
            #checkresume_div_n { display: none !important; }
        """.trimIndent().replace("\n", " ")
        webView.evaluateJavascript("var style = document.createElement('style'); style.innerHTML = '$css'; document.head.appendChild(style);", null)
    }

    /**
     * Injects JavaScript to attempt autoplay.
     */
    private fun injectAutoPlay() {
        val js = """
            (function() {
                var attempts = 0;
                var interval = setInterval(function() {
                    var v = document.querySelector('video');
                    
                    // STOP if video is already playing
                    if (v && !v.paused && v.currentTime > 0) {
                        clearInterval(interval);
                        return;
                    }

                    // 1. Click Center (To clear ad overlays)
                    var centerEl = document.elementFromPoint(window.innerWidth / 2, window.innerHeight / 2);
                    if(centerEl && centerEl !== v) centerEl.click();
                    
                    setTimeout(function() {
                        // 2. Click Play Button (We rely on browser's click handler now)
                        var btn = document.querySelector('.vjs-play-control') || 
                                  document.querySelector('.jw-icon-play') ||     
                                  document.querySelector('button[aria-label="Play"]') ||
                                  document.querySelector('button[title="Play"]');

                        if (btn) { 
                            btn.click(); 
                        } else if (v && v.paused) {
                            v.muted = false; 
                            v.play(); 
                        }
                    }, 300);
                    
                    attempts++;
                    if (attempts > 20) clearInterval(interval);
                }, 500);
            })();
        """.trimIndent().replace("\n", " ")
        webView.evaluateJavascript(js, null)
    }

    /**
     * Hides the system UI to enable immersive fullscreen mode.
     */
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    /**
     * Called when the activity is stopped.
     *
     * Destroys the WebView to release resources.
     */
    override fun onStop() {
        super.onStop()
        webView.destroy()
    }
}
