package com.ncflix.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
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
 * Optimized with [ListAdapter] and [DiffUtil] for efficient updates and animations.
 *
 * @property onMovieClick The callback function to be invoked when a movie item is clicked.
 */
class MovieAdapter(
    private val onMovieClick: (Movie) -> Unit
) : ListAdapter<Movie, MovieAdapter.MovieViewHolder>(MovieDiffCallback) {

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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movie, parent, false)
        val holder = MovieViewHolder(view)

        // Optimization: Set listener here to avoid object allocation in onBindViewHolder
        view.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onMovieClick(getItem(position))
            }
        }
        return holder
    }

    /**
     * Called by the RecyclerView to display the data at the specified position.
     *
     * This method loads the movie poster using Coil.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = getItem(position)

        // Load image using Coil library
        holder.poster.load(movie.posterUrl) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
        }
    }

    companion object {
        private val MovieDiffCallback = object : DiffUtil.ItemCallback<Movie>() {
            override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
                // Use pageLink as a unique identifier for the movie
                return oldItem.pageLink == newItem.pageLink
            }

            override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
                // Data class equals() handles content comparison
                return oldItem == newItem
            }
        }
    }
}
