package com.ncflix.app.model

import java.io.Serializable

data class Movie(
    val title: String,
    val posterUrl: String,
    val pageLink: String,
    val description: String = "",
    val seasonTitle: String = "" // e.g. "Season 1"
) : Serializable