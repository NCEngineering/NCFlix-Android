package com.ncflix.app.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ncflix.app.R
import com.ncflix.app.adapter.MovieAdapter
import com.ncflix.app.data.MovieRepository
import com.ncflix.app.model.Movie
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val repository = MovieRepository()
    private lateinit var adapter: MovieAdapter
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val imgHero = findViewById<ImageView>(R.id.imgHero)
        val txtHeroTitle = findViewById<TextView>(R.id.txtHeroTitle)
        val btnPlayHero = findViewById<Button>(R.id.btnPlayHero)
        val rvMovies = findViewById<RecyclerView>(R.id.rvMovies)
        val groupHero = findViewById<Group>(R.id.groupHero)
        val searchView = findViewById<SearchView>(R.id.searchView)
        val lblListHeader = findViewById<TextView>(R.id.lblListHeader)

        rvMovies.layoutManager = GridLayoutManager(this, 3)

        adapter = MovieAdapter(emptyList()) { selectedMovie ->
            openPlayer(selectedMovie)
        }
        rvMovies.adapter = adapter

        lifecycleScope.launch {
            val (heroMovie, trendingList) = repository.fetchHomeData()
            if (heroMovie != null) {
                imgHero.load(heroMovie.posterUrl)
                txtHeroTitle.text = heroMovie.title
                btnPlayHero.setOnClickListener { openPlayer(heroMovie) }

                adapter = MovieAdapter(trendingList) { openPlayer(it) }
                rvMovies.adapter = adapter
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it, groupHero, lblListHeader, rvMovies) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean = false
        })
    }

    private fun performSearch(query: String, heroGroup: Group, header: TextView, rv: RecyclerView) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Searching...", Toast.LENGTH_SHORT).show()
            heroGroup.visibility = View.GONE
            header.text = "Search Results: $query"

            val results = repository.searchMovies(query)

            if (results.isNotEmpty()) {
                adapter = MovieAdapter(results) { openPlayer(it) }
                rv.adapter = adapter
            } else {
                Toast.makeText(this@MainActivity, "No movies found", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openPlayer(movie: Movie) {
        Log.d("NC-FLIX", "Opening: ${movie.title} | Link: ${movie.pageLink}")

        // SMART ROUTING FIX: Check for /series/ in the URL
        if (movie.pageLink.contains("/series/", ignoreCase = true)) {
            Log.d("NC-FLIX", "Identified as SERIES. Opening Episode List.")
            val intent = Intent(this, EpisodeActivity::class.java)
            intent.putExtra("SERIES", movie)
            startActivity(intent)
        } else {
            Log.d("NC-FLIX", "Identified as MOVIE/EPISODE. Opening Player.")
            // Use PlayerActivity directly (which contains the WebView for playback)
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("EPISODE_URL", movie.pageLink)
            startActivity(intent)
        }
    }
}