package com.ncflix.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ncflix.app.R
import com.ncflix.app.model.Movie

/**
 * RecyclerView Adapter for displaying a list of episodes.
 *
 * This adapter handles the binding of episode data to the UI views in the RecyclerView.
 * It also handles item click events.
 *
 * @property episodes The list of [Movie] objects representing the episodes.
 * @property onClick The callback function to be invoked when an episode is clicked.
 */
class EpisodeAdapter(
    private val episodes: List<Movie>,
    private val onClick: (Movie) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder>() {

    /**
     * ViewHolder for an episode item.
     *
     * Holds references to the views within an item layout to avoid repeated `findViewById` calls.
     *
     * @param view The root view of the item layout.
     */
    class EpisodeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        /**
         * The TextView displaying the episode title.
         */
        val title: TextView = view.findViewById(R.id.txtEpisodeTitle)
    }

    /**
     * Called when the RecyclerView needs a new [EpisodeViewHolder] of the given type to represent
     * an item.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new [EpisodeViewHolder] that holds a View of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        val holder = EpisodeViewHolder(view)

        // Optimization: Set listener here to avoid object allocation in onBindViewHolder
        // This is a known pattern in this codebase (see MovieAdapter)
        view.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onClick(episodes[position])
            }
        }
        return holder
    }

    /**
     * Called by the RecyclerView to display the data at the specified position.
     *
     * This method updates the contents of the [EpisodeViewHolder.itemView] to reflect the item at
     * the given position.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val episode = episodes[position]
        holder.title.text = episode.title
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount() = episodes.size
}
