package com.ncflix.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.google.android.material.button.MaterialButton
import com.ncflix.app.R
import com.ncflix.app.model.Movie

class DetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        val movie = intent.getSerializableExtra("MOVIE") as? Movie
        if (movie == null) {
            finish()
            return
        }

        val imgPoster: ImageView = findViewById(R.id.imgPoster)
        val txtTitle: TextView = findViewById(R.id.txtTitle)
        val txtMeta: TextView = findViewById(R.id.txtMeta)
        val txtDescription: TextView = findViewById(R.id.txtDescription)
        val btnPlay: MaterialButton = findViewById(R.id.btnPlay)
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)

        toolbar.setNavigationOnClickListener { finish() }

        txtTitle.text = movie.title
        // Use description for metadata (Year/Rating) if available, otherwise generic
        txtMeta.text = movie.description.ifEmpty { "Movie" }
        txtDescription.text = "${movie.title} is a top-rated movie. Watch it now on NC-Flix."
        
        if (movie.posterUrl.isNotEmpty()) {
            imgPoster.load(movie.posterUrl)
        }

        btnPlay.setOnClickListener {
            // If it's an IMDb item, we need to search for it.
            // If it's a regular item, we play/open episodes.
            // For simplicity, we return a result or start MainActivity with search intent.
            
            if (movie.pageLink.startsWith("search:")) {
                // It's an IMDb/Search item
                val query = movie.pageLink.removePrefix("search:")
                val intent = Intent(this, MainActivity::class.java)
                intent.action = "ACTION_SEARCH"
                intent.putExtra("QUERY", query)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            } else {
                // Regular item
                val intent = Intent(this, EpisodeActivity::class.java)
                intent.putExtra("SERIES", movie)
                startActivity(intent)
            }
        }
    }
}