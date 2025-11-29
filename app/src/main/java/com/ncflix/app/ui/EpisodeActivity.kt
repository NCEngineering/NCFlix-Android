package com.ncflix.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ncflix.app.R
import com.ncflix.app.adapter.EpisodeAdapter
import com.ncflix.app.data.MovieRepository
import com.ncflix.app.model.Movie
import kotlinx.coroutines.launch

/**
 * Activity for displaying the details of a TV series, including its seasons and episodes.
 *
 * This activity receives a [Movie] object representing the series via an Intent.
 * It fetches the available seasons and episodes, displays them, and allows the user to
 * select a season to filter episodes. Clicking an episode launches the [PlayerActivity].
 */
class EpisodeActivity : AppCompatActivity() {

    private lateinit var rvEpisodes: RecyclerView
    private lateinit var spinnerSeason: Spinner
    private lateinit var txtTitle: TextView
    private val repository = MovieRepository()

    // Store all data: Map of "Season Name" -> List of Episodes
    private var allSeasonsData: Map<String, List<Movie>> = emptyMap()

    /**
     * Called when the activity is starting.
     *
     * Initializes the UI components, retrieves the series data from the intent,
     * and initiates the fetching of season and episode data.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in [onSaveInstanceState].  <b><i>Note: Otherwise it is null.</i></b>
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_episode)

        rvEpisodes = findViewById(R.id.rvEpisodes)
        spinnerSeason = findViewById(R.id.spinnerSeason)
        txtTitle = findViewById(R.id.txtSeriesTitle)

        // 1. Get the Series Object passed from MainActivity
        val series = intent.getSerializableExtra("SERIES") as? Movie

        if (series == null) {
            Toast.makeText(this, "Error: Could not load series info", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        txtTitle.text = series.title
        rvEpisodes.layoutManager = LinearLayoutManager(this)

        fetchSeasons(series.pageLink)
    }

    /**
     * Fetches the seasons and episodes for the series from the repository.
     *
     * This method runs asynchronously using [lifecycleScope]. Upon successful completion,
     * it calls [setupSeasonDropdown]. If no episodes are found, it displays a toast message.
     *
     * @param url The URL of the series page.
     */
    private fun fetchSeasons(url: String) {
        lifecycleScope.launch {
            Toast.makeText(this@EpisodeActivity, "Loading Seasons...", Toast.LENGTH_SHORT).show()

            // Fetch grouped data
            allSeasonsData = repository.fetchEpisodes(url)

            if (allSeasonsData.isNotEmpty()) {
                setupSeasonDropdown()
            } else {
                Toast.makeText(this@EpisodeActivity, "No episodes found.", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Configures the season selection dropdown (Spinner).
     *
     * If multiple seasons are available, it populates the spinner and sets up a listener
     * to update the episode list when a season is selected. If only one season exists,
     * the spinner is hidden and the episode list is updated immediately.
     */
    private fun setupSeasonDropdown() {
        val seasonNames = allSeasonsData.keys.toList()

        if (seasonNames.size > 1) {
            // Multiple Seasons: Show dropdown and allow switching
            spinnerSeason.visibility = View.VISIBLE
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, seasonNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSeason.adapter = adapter

            spinnerSeason.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedSeason = seasonNames[position]
                    updateEpisodeList(selectedSeason)
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
            // Load the first season initially
            updateEpisodeList(seasonNames.first())

        } else {
            // Only One Season: Hide dropdown and show all episodes
            spinnerSeason.visibility = View.GONE
            val singleSeasonName = seasonNames.firstOrNull() ?: return
            updateEpisodeList(singleSeasonName)
        }
    }

    /**
     * Updates the RecyclerView with the list of episodes for the specified season.
     *
     * @param seasonName The name of the season to display (key in [allSeasonsData]).
     */
    private fun updateEpisodeList(seasonName: String) {
        val episodes = allSeasonsData[seasonName] ?: emptyList()

        // Ensure EpisodeAdapter is used here to display titles
        rvEpisodes.adapter = EpisodeAdapter(episodes) { episode ->
            // Routing to PlayerActivity
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("EPISODE_URL", episode.pageLink)
            startActivity(intent)
        }
    }
}
