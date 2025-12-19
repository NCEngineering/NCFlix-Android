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

import android.app.AlertDialog
import android.net.Uri
import com.ncflix.app.utils.UpdateChecker

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var imgHero: ImageView
    private lateinit var txtHeroTitle: TextView
    private lateinit var btnPlayHero: Button
    private lateinit var rvTrending: RecyclerView
    private lateinit var rvSeries: RecyclerView
    private lateinit var rvMovies: RecyclerView
    private lateinit var rvMostViewed: RecyclerView
    private lateinit var rvMalaysia: RecyclerView
    private lateinit var loadingIndicator: ProgressBar

    private lateinit var homeSectionViews: List<View>

    // Adapters as properties to reuse instances
    private lateinit var trendingAdapter: MovieAdapter
    private lateinit var seriesAdapter: MovieAdapter
    private lateinit var moviesAdapter: MovieAdapter
    private lateinit var mostViewedAdapter: MovieAdapter
    private lateinit var malaysiaAdapter: MovieAdapter
    // Search adapter shares the trending recycler view, so we can reuse trendingAdapter or create a specific one
    // In this app logic, rvTrending is used for search results.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            // Initialize Views
            imgHero = findViewById(R.id.imgHero)
            txtHeroTitle = findViewById(R.id.txtHeroTitle)
            btnPlayHero = findViewById(R.id.btnPlayHero)
            rvTrending = findViewById(R.id.rvTrending)
            rvSeries = findViewById(R.id.rvSeries)
            rvMovies = findViewById(R.id.rvMovies)
            rvMostViewed = findViewById(R.id.rvMostViewed)
            rvMalaysia = findViewById(R.id.rvMalaysia)
            loadingIndicator = findViewById(R.id.loadingIndicator)
            val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
            val groupHero = findViewById<View>(R.id.groupHero)
            val lblListHeader = findViewById<TextView>(R.id.lblListHeader)
            
            // Search UI Components
            val btnSearchTop = findViewById<ImageView>(R.id.btnSearchTop)
            val containerSearch = findViewById<View>(R.id.containerSearch)
            
            // Update UI Components
            val btnUpdateTop = findViewById<ImageView>(R.id.btnUpdateTop)

            // Set Version Text
            val txtVersion = findViewById<TextView>(R.id.txtVersion)
            var currentVersionName = "1.0"
            try {
                val pInfo = packageManager.getPackageInfo(packageName, 0)
                currentVersionName = pInfo.versionName ?: "1.0"
                txtVersion.text = "v$currentVersionName"
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Initialize Adapters
            trendingAdapter = MovieAdapter { openPlayer(it) }
            seriesAdapter = MovieAdapter { openPlayer(it) }
            moviesAdapter = MovieAdapter { openPlayer(it) }
            mostViewedAdapter = MovieAdapter { openPlayer(it) }
            malaysiaAdapter = MovieAdapter { openPlayer(it) }

            // Attach Adapters to RecyclerViews
            rvTrending.adapter = trendingAdapter
            rvSeries.adapter = seriesAdapter
            rvMovies.adapter = moviesAdapter
            rvMostViewed.adapter = mostViewedAdapter
            rvMalaysia.adapter = malaysiaAdapter

            // Initialize list with explicit type to avoid ArrayStoreException
            homeSectionViews = listOf<View>(
                rvTrending, rvSeries, rvMovies, rvMostViewed, rvMalaysia,
                findViewById(R.id.lblSeries), findViewById(R.id.lblMovies),
                findViewById(R.id.lblMostViewed), findViewById(R.id.lblMalaysia)
            )
            
            // Search Button Click Listener
            btnSearchTop.setOnClickListener {
                containerSearch.visibility = View.VISIBLE
                searchView.isIconified = false
                searchView.requestFocus()
            }
            
            // Update Button Click Listener
            btnUpdateTop.setOnClickListener {
                lifecycleScope.launch {
                    Toast.makeText(this@MainActivity, "Checking for updates...", Toast.LENGTH_SHORT).show()
                    when (val result = UpdateChecker.checkForUpdate(currentVersionName)) {
                        is UpdateChecker.UpdateResult.Available -> {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Update Available")
                                .setMessage("Version ${result.version} is available. Download now?")
                                .setPositiveButton("Download") { _, _ ->
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(result.url))
                                    startActivity(browserIntent)
                                }
                                .setNegativeButton("Later", null)
                                .show()
                        }
                        is UpdateChecker.UpdateResult.NoUpdate -> {
                            Toast.makeText(this@MainActivity, "App is up to date", Toast.LENGTH_SHORT).show()
                        }
                        is UpdateChecker.UpdateResult.Error -> {
                            Toast.makeText(this@MainActivity, "Error: ${result.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            // Handle Search Intent (from DetailsActivity)
            handleIntent(intent, searchView)

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
                containerSearch.visibility = View.GONE
                false
            }

            // Observe State
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    launch {
                        viewModel.homeState.collect { state ->
                            if (viewModel.searchState.value == null) {
                                updateHomeUI(state, groupHero, lblListHeader)
                            }
                        }
                    }
                    
                    launch {
                        viewModel.seriesState.collect { state ->
                            if (viewModel.searchState.value == null && state is Resource.Success) {
                                seriesAdapter.submitList(state.data)
                                rvSeries.isVisible = true
                            }
                        }
                    }

                    launch {
                        viewModel.moviesState.collect { state ->
                            if (viewModel.searchState.value == null && state is Resource.Success) {
                                moviesAdapter.submitList(state.data)
                                rvMovies.isVisible = true
                            }
                        }
                    }

                    launch {
                        viewModel.mostViewedState.collect { state ->
                            if (viewModel.searchState.value == null && state is Resource.Success) {
                                mostViewedAdapter.submitList(state.data)
                                rvMostViewed.isVisible = true
                            }
                        }
                    }

                    launch {
                        viewModel.malaysiaState.collect { state ->
                            if (viewModel.searchState.value == null && state is Resource.Success) {
                                malaysiaAdapter.submitList(state.data)
                                rvMalaysia.isVisible = true
                            }
                        }
                    }

                    launch {
                        viewModel.searchState.collect { state ->
                            if (state != null) {
                                // Search Mode: Hide ALL home sections
                                groupHero.visibility = View.GONE
                                lblListHeader.text = getString(R.string.search_hint)
                                homeSectionViews.forEach { it?.visibility = View.GONE }

                                when (state) {
                                    is Resource.Loading -> {
                                        loadingIndicator.visibility = View.VISIBLE
                                        rvTrending.visibility = View.GONE
                                    }
                                    is Resource.Success -> {
                                        loadingIndicator.visibility = View.GONE
                                        rvTrending.visibility = View.VISIBLE

                                        // Reuse trendingAdapter for search results
                                        trendingAdapter.submitList(state.data)

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
                                
                                // Re-show headers and lists if data exists
                                homeSectionViews.forEach { it?.visibility = View.VISIBLE }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error in onCreate: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val searchView = findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)
        handleIntent(intent, searchView)
    }

    private fun handleIntent(intent: Intent?, searchView: androidx.appcompat.widget.SearchView) {
        if (intent?.action == "ACTION_SEARCH") {
            val query = intent.getStringExtra("QUERY")
            if (!query.isNullOrBlank()) {
                searchView.setQuery(query, false) // Set text in search bar
                searchView.isIconified = false // Expand search view
                viewModel.searchMovies(query)
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

                trendingAdapter.submitList(trendingList)
            }
            is Resource.Error -> {
                loadingIndicator.visibility = View.GONE
                Toast.makeText(this, "Failed to load: ${state.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openPlayer(movie: Movie) {
        // PASS 1: Go to Details Screen first
        val intent = Intent(this, DetailsActivity::class.java)
        intent.putExtra("MOVIE", movie) // Key must match DetailsActivity
        startActivity(intent)
    }
}
