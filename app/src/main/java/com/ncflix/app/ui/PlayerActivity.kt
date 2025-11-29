package com.ncflix.app.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ncflix.app.R
import com.ncflix.app.utils.Resource
import com.ncflix.app.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    private val viewModel: PlayerViewModel by viewModels()
    
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtStatus: TextView
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerView = findViewById(R.id.playerView)
        progressBar = findViewById(R.id.progressBar)
        txtStatus = findViewById(R.id.txtStatus)

        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        playerView.controllerAutoShow = true

        hideSystemUI()

        val episodeUrl = intent.getStringExtra("EPISODE_URL")

        if (episodeUrl != null) {
            viewModel.loadStream(episodeUrl)
        } else {
            showError("Error: No URL provided")
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.streamState.collect { state ->
                    when (state) {
                        is Resource.Loading -> {
                            txtStatus.text = "Finding best server..."
                            txtStatus.visibility = View.VISIBLE
                            progressBar.visibility = View.VISIBLE
                        }
                        is Resource.Success -> {
                            progressBar.visibility = View.GONE
                            txtStatus.visibility = View.GONE
                            initializePlayer(state.data)
                        }
                        is Resource.Error -> {
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun showError(msg: String) {
        progressBar.visibility = View.GONE
        txtStatus.visibility = View.VISIBLE
        txtStatus.text = msg
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    @OptIn(UnstableApi::class)
    private fun initializePlayer(url: String) {
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Referer" to "https://ww93.pencurimovie.bond/"
        )

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        playerView.player = player

        val mediaItem = MediaItem.fromUri(url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true

        player?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                showError("Playback Error: ${error.message}")
                error.printStackTrace()
            }
        })
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}