package com.ncflix.app.model

import java.io.Serializable

/**
 * Represents a movie or TV show entity within the application.
 *
 * This data class encapsulates all the necessary information about a movie,
 * including its title, visual representation (poster), and link to more details.
 * It implements [Serializable] to allow passing object instances between components.
 *
 * @property title The title of the movie or TV show.
 * @property posterUrl The URL string pointing to the movie's poster image.
 * @property pageLink The URL string or deep link to the movie's detail page.
 * @property description A brief summary or plot description of the movie. Defaults to an empty string.
 * @property seasonTitle The title of the specific season if applicable (e.g., "Season 1"). Defaults to an empty string.
 */
data class Movie(
    val title: String,
    val posterUrl: String,
    val pageLink: String,
    val description: String = "",
    val seasonTitle: String = "" // e.g. "Season 1"
) : Serializable
