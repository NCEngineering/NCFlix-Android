package com.ncflix.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ncflix.app.R
import com.ncflix.app.adapter.EpisodeAdapter
import com.ncflix.app.model.Movie
import com.ncflix.app.utils.Resource
import com.ncflix.app.viewmodel.EpisodeViewModel
import kotlinx.coroutines.launch

class EpisodeActivity : AppCompatActivity() {

    private val viewModel: EpisodeViewModel by viewModels()
    
    private lateinit var rvEpisodes: RecyclerView
    private lateinit var spinnerSeason: Spinner
    private lateinit var txtTitle: TextView

    private var allSeasonsData: Map<String, List<Movie>> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_episode)

        rvEpisodes = findViewById(R.id.rvEpisodes)
        spinnerSeason = findViewById(R.id.spinnerSeason)
        txtTitle = findViewById(R.id.txtSeriesTitle)

        val series = intent.getSerializableExtra("SERIES") as? Movie

        if (series == null) {
            Toast.makeText(this, "Error: Could not load series info", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        txtTitle.text = series.title
        rvEpisodes.layoutManager = LinearLayoutManager(this)

        // Trigger fetch
        viewModel.loadEpisodes(series.pageLink)

        // Observe State
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.episodesState.collect { state ->
                    when (state) {
                        is Resource.Loading -> {
                             // Show loading if needed
                        }
                        is Resource.Success -> {
                            allSeasonsData = state.data
                            setupSeasonDropdown()
                        }
                        is Resource.Error -> {
                            Toast.makeText(this@EpisodeActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupSeasonDropdown() {
        val seasonNames = allSeasonsData.keys.toList()

        if (seasonNames.size > 1) {
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
            updateEpisodeList(seasonNames.first())

        } else {
            spinnerSeason.visibility = View.GONE
            val singleSeasonName = seasonNames.firstOrNull()
            if (singleSeasonName != null) {
                val episodes = allSeasonsData[singleSeasonName] ?: emptyList()
                if (episodes.size == 1) {
                    val episode = episodes[0]
                    val intent = Intent(this, PlayerActivity::class.java)
                    intent.putExtra("EPISODE_URL", episode.pageLink)
                    startActivity(intent)
                    finish()
                    return
                }
                updateEpisodeList(singleSeasonName)
            }
        }
    }

    private fun updateEpisodeList(seasonName: String) {
        val episodes = allSeasonsData[seasonName] ?: emptyList()

        rvEpisodes.adapter = EpisodeAdapter(episodes) { episode ->
            val intent = Intent(this, PlayerActivity::class.java)
            intent.putExtra("EPISODE_URL", episode.pageLink)
            startActivity(intent)
        }
    }
}