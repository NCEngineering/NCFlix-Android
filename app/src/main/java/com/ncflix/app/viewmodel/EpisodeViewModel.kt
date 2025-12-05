package com.ncflix.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncflix.app.data.MovieRepository
import com.ncflix.app.model.Movie
import com.ncflix.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EpisodeViewModel : ViewModel() {
    private val repository = MovieRepository()

    private val _episodesState = MutableStateFlow<Resource<Map<String, List<Movie>>>>(Resource.Loading)
    val episodesState: StateFlow<Resource<Map<String, List<Movie>>>> = _episodesState.asStateFlow()

    fun loadEpisodes(url: String) {
        viewModelScope.launch {
            _episodesState.value = Resource.Loading
            _episodesState.value = repository.getEpisodes(url)
        }
    }
}