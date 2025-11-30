package com.ncflix.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ncflix.app.BuildConfig
import com.ncflix.app.R
import com.ncflix.app.adapter.MovieAdapter
import com.ncflix.app.model.Movie
import com.ncflix.app.utils.Resource
import com.ncflix.app.utils.UpdateChecker
import com.ncflix.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Views
    private lateinit var mainScrollView: View
    private lateinit var containerSearch: View
    private lateinit var loadingIndicator: ProgressBar
    
    // Home Views
    private lateinit var imgHero: ImageView
    private lateinit var txtHeroTitle: TextView
    private lateinit var txtHeroGenre: TextView
    private lateinit var btnPlayHero: Button
    private lateinit var rvTrending: RecyclerView
    private lateinit var rvSeries: RecyclerView
    private lateinit var rvMovies: RecyclerView
    private lateinit var rvMostViewed: RecyclerView
    private lateinit var rvMalaysia: RecyclerView
    
    // Search Views
    private lateinit var searchView: SearchView
    private lateinit var rvSearchResults: RecyclerView
    
    // Navigation
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var btnUpdateTop: ImageView
    private lateinit var btnSearchTop: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupListeners()
        handleIntent(intent)
        
        // Observe State
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.homeState.collect { handleHomeState(it) } }
                launch { viewModel.searchState.collect { if (it != null) handleSearchState(it) } }
                
                // New Lists
                launch { viewModel.seriesState.collect { updateList(it, rvSeries) } }
                launch { viewModel.moviesState.collect { updateList(it, rvMovies) } }
                launch { viewModel.mostViewedState.collect { updateList(it, rvMostViewed) } }
                launch { viewModel.malaysiaState.collect { updateList(it, rvMalaysia) } }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "ACTION_SEARCH") {
            val query = intent.getStringExtra("QUERY")
            if (!query.isNullOrEmpty()) {
                toggleSearch(true)
                searchView.setQuery(query, true)
            }
        }
    }

    private fun initializeViews() {
        mainScrollView = findViewById(R.id.mainScrollView)
        containerSearch = findViewById(R.id.containerSearch)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        
        imgHero = findViewById(R.id.imgHero)
        txtHeroTitle = findViewById(R.id.txtHeroTitle)
        txtHeroGenre = findViewById(R.id.txtHeroGenre)
        btnPlayHero = findViewById(R.id.btnPlayHero)
        rvTrending = findViewById(R.id.rvTrending)
        rvSeries = findViewById(R.id.rvSeries)
        rvMovies = findViewById(R.id.rvMovies)
        rvMostViewed = findViewById(R.id.rvMostViewed)
        rvMalaysia = findViewById(R.id.rvMalaysia)
        
        searchView = findViewById(R.id.searchView)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        
        bottomNavigation = findViewById(R.id.bottomNavigation)
        btnUpdateTop = findViewById(R.id.btnUpdateTop)
        btnSearchTop = findViewById(R.id.btnSearchTop)
    }

    private fun setupListeners() {
        // Bottom Nav
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    toggleSearch(false, true)
                    true
                }
                R.id.navigation_search -> {
                    toggleSearch(true, true)
                    true
                }
                R.id.navigation_update -> {
                    checkForUpdates()
                    false // Don't select the item visually, just perform action
                }
                else -> false
            }
        }

        // Top Bar Icons
        btnSearchTop.setOnClickListener { toggleSearch(true) }
        btnUpdateTop.setOnClickListener { checkForUpdates() }

        // Hero Play
        btnPlayHero.setOnClickListener {
            val state = viewModel.homeState.value
            if (state is Resource.Success) {
                openPlayer(state.data.first)
            }
        }

        // Search View
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    viewModel.searchMovies(query)
                    searchView.clearFocus()
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                     // If user clears search, show New & Hot suggestions
                     viewModel.loadNewAndHot()
                }
                return true
            }
        })
    }
    
    private fun toggleSearch(show: Boolean, fromBottomNav: Boolean = false) {
        if (show) {
            mainScrollView.visibility = View.GONE
            containerSearch.visibility = View.VISIBLE
            
            if (!fromBottomNav) {
                bottomNavigation.selectedItemId = R.id.navigation_search
            }
            
            // Load default content for the "New & Hot" / Search tab
            if (searchView.query.isNullOrEmpty()) {
                viewModel.loadNewAndHot()
            }
        } else {
            mainScrollView.visibility = View.VISIBLE
            containerSearch.visibility = View.GONE
            
            if (!fromBottomNav) {
                bottomNavigation.selectedItemId = R.id.navigation_home
            }
            
            viewModel.clearSearch()
            // Clear search text to reset state
            searchView.setQuery("", false)
            searchView.clearFocus()
        }
    }

    private fun handleHomeState(state: Resource<Pair<Movie, List<Movie>>>) {
        when (state) {
            is Resource.Loading -> {
                if (mainScrollView.isVisible) loadingIndicator.visibility = View.VISIBLE
            }
            is Resource.Success -> {
                loadingIndicator.visibility = View.GONE
                val (heroMovie, trendingList) = state.data
                
                // Bind Hero
                imgHero.load(heroMovie.posterUrl)
                txtHeroTitle.text = heroMovie.title
                
                // Bind Trending
                rvTrending.adapter = MovieAdapter(trendingList) { openPlayer(it) }
            }
            is Resource.Error -> {
                loadingIndicator.visibility = View.GONE
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateList(state: Resource<List<Movie>>, recyclerView: RecyclerView) {
        if (state is Resource.Success) {
            recyclerView.adapter = MovieAdapter(state.data) { openPlayer(it) }
        }
    }

    private fun handleSearchState(state: Resource<List<Movie>>) {
        when (state) {
            is Resource.Loading -> {
                loadingIndicator.visibility = View.VISIBLE
            }
            is Resource.Success -> {
                loadingIndicator.visibility = View.GONE
                rvSearchResults.adapter = MovieAdapter(state.data) { openPlayer(it) }
            }
            is Resource.Error -> {
                loadingIndicator.visibility = View.GONE
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun checkForUpdates() {
        Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            when (val result = UpdateChecker.checkForUpdate(BuildConfig.VERSION_NAME)) {
                is UpdateChecker.UpdateResult.Available -> {
                    showUpdateDialog(result.version, result.url)
                }
                is UpdateChecker.UpdateResult.NoUpdate -> {
                    Toast.makeText(this@MainActivity, "You are on the latest version!", Toast.LENGTH_SHORT).show()
                }
                is UpdateChecker.UpdateResult.Error -> {
                    Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showUpdateDialog(version: String, url: String) {
        AlertDialog.Builder(this)
            .setTitle("Update Available")
            .setMessage("A new version ($version) of NCFlix is available.")
            .setPositiveButton("Update") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openPlayer(movie: Movie) {
        if (movie.pageLink.startsWith("search:")) {
            // Open Details Activity for these items (e.g. IMDb scraped items)
            val intent = Intent(this, DetailsActivity::class.java)
            intent.putExtra("MOVIE", movie)
            startActivity(intent)
            return
        }

        val episodeIntent = Intent(this, EpisodeActivity::class.java)
        episodeIntent.putExtra("SERIES", movie)
        startActivity(episodeIntent)
    }
}