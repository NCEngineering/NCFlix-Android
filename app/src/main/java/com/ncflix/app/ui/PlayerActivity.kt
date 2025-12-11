package com.ncflix.app.ui

import android.content.Intent
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ncflix.app.R
import com.ncflix.app.utils.Resource
import com.ncflix.app.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.ByteArrayInputStream
import java.lang.ref.WeakReference
import com.ncflix.app.utils.AdBlocker
import com.ncflix.app.utils.Constants
import androidx.activity.viewModels

class PlayerActivity : AppCompatActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    // Views
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtStatus: TextView
    private lateinit var txtGestureFeedback: TextView
    private lateinit var panelVideoDetected: View
    private lateinit var btnPlayNative: android.widget.Button
    private lateinit var btnCopyLink: android.widget.Button

    private lateinit var gestureDetector: GestureDetectorCompat

    // State
    private var serverList = ArrayList<String>()
    private var currentServerIndex = 0
    private var currentHost: String = ""
    private var isVideoPlaying = false
    private var capturedVideoUrl: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        setContentView(R.layout.activity_player)

        // Initialize Views
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        txtStatus = findViewById(R.id.txtStatus)
        txtGestureFeedback = findViewById(R.id.txtGestureFeedback)
        
        // New Panel UI
        panelVideoDetected = findViewById(R.id.panelVideoDetected)
        btnPlayNative = findViewById(R.id.btnPlayNative)
        btnCopyLink = findViewById(R.id.btnCopyLink)

        btnPlayNative.setOnClickListener {
            capturedVideoUrl?.let { url ->
                val intent = Intent(this, NativePlayerActivity::class.java)
                intent.putExtra("VIDEO_URL", url)
                startActivity(intent)
            }
        }

        btnCopyLink.setOnClickListener {
             capturedVideoUrl?.let { url ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Video URL", url)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
             }
        }

        setupWebPlayer()
        setupGestures()

        val episodeUrl = intent.getStringExtra("EPISODE_URL")

        if (episodeUrl != null) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.streamState.collect { state ->
                        when (state) {
                            is Resource.Loading -> {
                                txtStatus.text = "Finding stream servers..."
                                txtStatus.visibility = View.VISIBLE
                                progressBar.visibility = View.VISIBLE
                            }
                            is Resource.Success -> {
                                serverList = state.data
                                loadCurrentServer()
                            }
                            is Resource.Error -> {
                                showError(state.message)
                            }
                        }
                    }
                }
            }
            viewModel.loadStream(episodeUrl)
        } else {
            showError("Error: No URL provided")
        }
    }

    private fun showError(msg: String) {
        progressBar.visibility = View.GONE
        txtStatus.visibility = View.VISIBLE
        txtStatus.text = msg
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun loadCurrentServer() {
        isVideoPlaying = false // Reset playback state

        if (currentServerIndex < serverList.size) {
            val url = serverList[currentServerIndex]
            try { currentHost = Uri.parse(url).host ?: "" } catch(e: Exception){}

            txtStatus.text = "Trying Server ${currentServerIndex + 1}..."
            txtStatus.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE

            println("NC-FLIX: Loading Embed -> $url")
            val headers = mapOf("Referer" to Constants.BASE_URL)
            webView.loadUrl(url, headers)
        } else {
            txtStatus.text = "Failed: All servers are dead."
            progressBar.visibility = View.GONE
        }
    }

    // --- WebView Configuration ---

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
            userAgentString = Constants.USER_AGENT
        }

        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean = false

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {}
            override fun onHideCustomView() {}
        }

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                // Allow everything unless it's explicitly an Ad
                if (url != null && AdBlocker.isAd(url)) {
                    println("NC-FLIX: Nav Blocked (Ad) -> $url")
                    return true
                }
                return false // Allow WebView to load the redirect
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                this@PlayerActivity.lifecycleScope.launch {
                    delay(1000)
                    if (isActive) {
                        injectCSS()
                        injectAutoPlay()
                        injectAdBlockOverrides()
                    }
                }

                // Delayed Error Detection
                this@PlayerActivity.lifecycleScope.launch {
                    delay(7000)
                    if (isActive && !isVideoPlaying) {
                        injectErrorDetector()
                    }
                }
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url.toString()
                
                // Sniffer Logic
                if (url != null) {
                    val lowerUrl = url.lowercase()
                    if (lowerUrl.contains(".mp4") || lowerUrl.contains(".m3u8") || lowerUrl.contains(".mkv") || lowerUrl.contains(".ts")) {
                         // Filter out small segments if possible, but for now capture all potential streams
                         if (!lowerUrl.contains("favicon") && !lowerUrl.contains(".png")) {
                             if (capturedVideoUrl != url) {
                                 capturedVideoUrl = url
                                 runOnUiThread {
                                     if (panelVideoDetected.visibility != View.VISIBLE) {
                                         panelVideoDetected.visibility = View.VISIBLE
                                         Toast.makeText(this@PlayerActivity, "Video Stream Captured!", Toast.LENGTH_SHORT).show()
                                     }
                                 }
                             }
                         }
                    }
                }

                if (AdBlocker.isAd(url)) {
                    println("NC-FLIX: Res Blocked (Ad) -> $url")
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
    }

    private fun injectAdBlockOverrides() {
        val js = AdBlocker.getDomBypasses()
        webView.evaluateJavascript(js, null)
    }

    // --- Gesture Logic ---

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        // Initialize gesture detector after webView is initialized in onCreate
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

    private fun seekForward() {
        webView.evaluateJavascript("document.querySelector('video').currentTime += 10;", null)
    }

    private fun seekBackward() {
        webView.evaluateJavascript("document.querySelector('video').currentTime -= 10;", null)
    }

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


    // --- JS Interface (Bridge) ---

    class WebAppInterface(activity: PlayerActivity) {
        private val activityRef = WeakReference(activity)

        @JavascriptInterface
        fun onErrorDetected() {
            val activity = activityRef.get() ?: return
            activity.runOnUiThread {
                if (!activity.isVideoPlaying) {
                    println("NC-FLIX: Dead Link Confirmed by JS (Visual Check)! Switching...")
                    activity.currentServerIndex++
                    activity.loadCurrentServer()
                }
            }
        }

        @JavascriptInterface
        fun onVideoStarted() {
            val activity = activityRef.get() ?: return
            activity.runOnUiThread {
                if (!activity.isVideoPlaying) {
                    println("NC-FLIX: Video Started! Locking server.")
                    activity.isVideoPlaying = true
                    activity.progressBar.visibility = View.GONE
                    activity.txtStatus.visibility = View.GONE
                }
            }
        }

        @JavascriptInterface
        fun onPlayPauseToggled(state: String) {
            val activity = activityRef.get() ?: return
            activity.runOnUiThread {
                if (state == "play") {
                    activity.showGestureFeedback("▶")
                } else if (state == "pause") {
                    activity.showGestureFeedback("⏸")
                }
            }
        }
    }

    // --- Native UI Handlers ---

    private fun injectErrorDetector() {
        val js = """
            (function() {
                var errorImg = document.querySelector('img[src*="no_video"]');
                var h1 = document.querySelector('h1');
                
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

    private fun injectCSS() {
        val css = """
            #loading,.loading,.ad-container,.popup,.banner,#ads,.jw-logo,.watermark,.eruda-container{display:none !important;}
            body, html { background-color: black !important; margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden !important; }
            video { width: 100% !important; height: 100vh !important; object-fit: contain; }
            
            /* VideoJS: Hide all controls except progress */
            .vjs-control-bar > *:not(.vjs-progress-control) { display: none !important; }
            .vjs-play-control, .vjs-volume-panel, .vjs-fullscreen-control, .vjs-subs-caps-button { display: none !important; }
            .vjs-progress-control { display: flex !important; opacity: 1 !important; pointer-events: auto !important; }
            
            /* JWPlayer: Hide buttons, keep time slider */
            .jw-controlbar-left-group, .jw-controlbar-right-group { display: none !important; }
            .jw-icon-playback, .jw-icon-volume, .jw-icon-fullscreen, .jw-icon-settings, .jw-icon-cc { display: none !important; }
            .jw-controlbar-center-group { display: flex !important; opacity: 1 !important; pointer-events: auto !important; }
            
            .vjs-big-play-button { display: none !important; }
            #checkresume_div_n { display: none !important; }
        """.trimIndent().replace("\n", " ") + " " + AdBlocker.getCssRules()
        
        webView.evaluateJavascript("var style = document.createElement('style'); style.innerHTML = '$css'; document.head.appendChild(style);", null)
    }

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
                        // 2. Click Play Button
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

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onStop() {
        super.onStop()
        webView.destroy()
    }
}
