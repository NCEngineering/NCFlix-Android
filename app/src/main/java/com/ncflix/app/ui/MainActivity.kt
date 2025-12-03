package com.ncflix.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ncflix.app.R
import com.ncflix.app.adapter.MovieAdapter
import com.ncflix.app.model.Movie
import com.ncflix.app.utils.Resource
import com.ncflix.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var imgHero: ImageView
    private lateinit var txtHeroTitle: TextView
    private lateinit var btnPlayHero: Button
    private lateinit var rvTrending: RecyclerView
    private lateinit var loadingIndicator: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        imgHero = findViewById(R.id.imgHero)
        txtHeroTitle = findViewById(R.id.txtHeroTitle)
        btnPlayHero = findViewById(R.id.btnPlayHero)
        rvTrending = findViewById(R.id.rvTrending)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        val groupHero = findViewById<View>(R.id.groupHero)
        val lblListHeader = findViewById<TextView>(R.id.lblListHeader)

        // Setup Search Listener
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.isNotBlank()) viewModel.searchMovies(it)
                }
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    viewModel.clearSearch()
                }
                return false
            }
        })

        // Handle Search Close
        searchView.setOnCloseListener {
            viewModel.clearSearch()
            false
        }

        // Observe State
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.homeState.collect { state ->
                        // Only show home state if search is not active (searchState is null)
                        if (viewModel.searchState.value == null) {
                            updateHomeUI(state, groupHero, lblListHeader)
                        }
                    }
                }

                launch {
                    viewModel.searchState.collect { state ->
                        if (state != null) {
                            // Search Mode
                            groupHero.visibility = View.GONE
                            lblListHeader.text = getString(R.string.search_hint) // Or "Search Results"

                            when (state) {
                                is Resource.Loading -> {
                                    loadingIndicator.visibility = View.VISIBLE
                                    rvTrending.visibility = View.GONE
                                }
                                is Resource.Success -> {
                                    loadingIndicator.visibility = View.GONE
                                    rvTrending.visibility = View.VISIBLE

                                    val adapter = MovieAdapter(state.data) { selectedMovie ->
                                        openPlayer(selectedMovie)
                                    }
                                    rvTrending.adapter = adapter

                                    if (state.data.isEmpty()) {
                                        Toast.makeText(this@MainActivity, "No results found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                is Resource.Error -> {
                                    loadingIndicator.visibility = View.GONE
                                    Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            // Home Mode (Re-apply home state if available)
                             val homeState = viewModel.homeState.value
                             updateHomeUI(homeState, groupHero, lblListHeader)
                        }
                    }
                }
            }
        }
    }

    private fun updateHomeUI(state: Resource<Pair<Movie, List<Movie>>>, groupHero: View, lblListHeader: TextView) {
        when (state) {
            is Resource.Loading -> {
                loadingIndicator.visibility = View.VISIBLE
                rvTrending.visibility = View.GONE
                groupHero.visibility = View.GONE
            }
            is Resource.Success -> {
                loadingIndicator.visibility = View.GONE
                rvTrending.visibility = View.VISIBLE
                groupHero.visibility = View.VISIBLE
                lblListHeader.text = getString(R.string.trending_now)

                val (heroMovie, trendingList) = state.data

                imgHero.load(heroMovie.posterUrl)
                txtHeroTitle.text = heroMovie.title
                btnPlayHero.setOnClickListener { openPlayer(heroMovie) }

                val adapter = MovieAdapter(trendingList) { selectedMovie ->
                    openPlayer(selectedMovie)
                }
                rvTrending.adapter = adapter
            }
            is Resource.Error -> {
                loadingIndicator.visibility = View.GONE
                // Don't show toast for home error if we are searching (handled by flow checks but safety first)
            }
        }
    }

    private fun openPlayer(movie: Movie) {
        val intent = Intent(this, PlayerActivity::class.java) // Changed from EpisodeActivity to PlayerActivity for direct play, logic can be improved to detect Series/Movie
        // Ideally, we check if it's a series. For now, let's assume if it has "Season" in title it's a series? 
        // Or just try to scrape episodes first. 
        // For simplicity in this iteration: Direct play if it's a movie, or go to EpisodeActivity if logic dictates.
        // Let's route to EpisodeActivity by default for detailed view if implemented, or keep it simple.
        
        // Let's use a simple heuristic: If it is homepage trending, it might be mixed.
        // But the user flow in README says "Watch Series: If you select a TV series..."
        // For now, let's route to EpisodeActivity if we want to support seasons, 
        // BUT EpisodeActivity needs to be robust.
        // Let's stick to PlayerActivity for now for the "Play" button, but for the list click?
        
        // Let's route to EpisodeActivity to check for seasons. If only 1 "episode" found (the movie), it auto-plays or shows 1 item.
        val episodeIntent = Intent(this, EpisodeActivity::class.java)
        episodeIntent.putExtra("SERIES", movie)
        startActivity(episodeIntent)
    }
}