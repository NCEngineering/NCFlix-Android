package com.ncflix.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ncflix.app.R
import com.ncflix.app.model.Movie

/**
 * RecyclerView Adapter for displaying a grid or list of movies.
 *
 * This adapter binds [Movie] objects to the corresponding views, primarily displaying the movie poster.
 * It uses the Coil library for image loading.
 *
 * @property movies The list of [Movie] objects to display.
 * @property onMovieClick The callback function to be invoked when a movie item is clicked.
 */
class MovieAdapter(
    private val movies: List<Movie>,
    private val onMovieClick: (Movie) -> Unit
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    /**
     * ViewHolder for a movie item.
     *
     * Caches the ImageView used to display the movie poster.
     *
     * @param view The root view of the item layout.
     */
    class MovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        /**
         * The ImageView displaying the movie poster.
         */
        val poster: ImageView = view.findViewById(R.id.itemPoster)
    }

    /**
     * Called when the RecyclerView needs a new [MovieViewHolder] of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new [MovieViewHolder] that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        // We need to create this layout file next!
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)
        return MovieViewHolder(view)
    }

    /**
     * Called by the RecyclerView to display the data at the specified position.
     *
     * This method loads the movie poster using Coil and sets up the click listener.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]

        // Load image using Coil library
        holder.poster.load(movie.posterUrl) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
        }

        holder.itemView.setOnClickListener {
            onMovieClick(movie)
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of movies in this adapter.
     */
    override fun getItemCount() = movies.size
}
